// src/main/kotlin/kr/hhplus/be/server/application/service/payment/PaymentCommandService.kt
package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand
import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.GetPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.GetUserPaymentsUseCase
import kr.hhplus.be.server.application.port.`in`.ProcessPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.log.PointHistory
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.payment.exception.InvalidPaymentAmountException
import kr.hhplus.be.server.domain.payment.exception.PaymentAlreadyProcessedException
import kr.hhplus.be.server.domain.payment.exception.PaymentNotFoundException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.InvalidReservationStatusException
import kr.hhplus.be.server.domain.reservation.exception.ReservationExpiredException
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class PaymentCommandService(
    private val paymentRepository: PaymentRepository,
    private val tempReservationRepository: TempReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val completeTokenUseCase: CompleteTokenUseCase
) : ProcessPaymentUseCase, GetPaymentUseCase, GetUserPaymentsUseCase {

    override fun processPayment(command: ProcessPaymentCommand): PaymentResult {
        if (command.pointsToUse < 0) {
            throw InvalidPaymentAmountException(command.pointsToUse)
        }

        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        val tempReservation = tempReservationRepository.findByTempReservationId(command.reservationId)
            ?: throw TempReservationNotFoundException(command.reservationId)

        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(
                tempReservation.status,
                TempReservationStatus.RESERVED
            )
        }

        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(command.reservationId)
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Reservation does not belong to this user")
        }

        val existingPayment = paymentRepository.findByReservationId(command.reservationId)
        if (existingPayment != null) {
            throw PaymentAlreadyProcessedException(command.reservationId)
        }

        val user = userRepository.findByUserId(token.userId)
            ?: throw UserNotFoundException(token.userId)

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val seatGrades = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, token.concertId)
        val seatGrade = seatGrades.firstOrNull()
            ?: throw ConcertNotFoundException(token.concertId)

        val totalAmount = seatGrade.price
        val pointsToUse = minOf(command.pointsToUse, user.availablePoint, totalAmount)
        val actualAmount = totalAmount - pointsToUse

        if (user.availablePoint < actualAmount) {
            throw InsufficientPointException(actualAmount, user.availablePoint)
        }

        val updatedUser = user.usePoint(actualAmount + pointsToUse)
        userRepository.save(updatedUser)

        val payment = Payment(
            paymentId = 0L,
            reservationId = command.reservationId,
            userId = token.userId,
            totalAmount = totalAmount,
            discountAmount = pointsToUse,
            actualAmount = actualAmount,
            paymentAt = LocalDateTime.now(),
            isCancel = false,
            isRefund = false,
            cancelAt = null
        )
        val savedPayment = paymentRepository.save(payment)

        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        val reservation = Reservation(
            reservationId = 0L,
            userId = token.userId,
            concertDateId = seat.concertDateId,
            seatId = tempReservation.concertSeatId,
            reservationAt = System.currentTimeMillis(),
            cancelAt = 0,
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = actualAmount
        )
        reservationRepository.save(reservation)

        if (pointsToUse > 0) {
            val pointHistory = PointHistory(
                pointHistoryId = 0L,
                userId = token.userId,
                pointHistoryType = "USED",
                pointHistoryAmount = pointsToUse,
                description = "Concert ticket payment"
            )
            pointHistoryRepository.save(pointHistory)
        }

        completeTokenUseCase.completeToken(CompleteTokenCommand(command.tokenId))

        return PaymentMapper.toResult(savedPayment, "Payment completed successfully")
    }

    @Transactional(readOnly = true)
    override fun getPayment(command: GetPaymentCommand): PaymentResult {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        val payment = paymentRepository.findByPaymentId(command.paymentId)
            ?: throw PaymentNotFoundException(command.paymentId)

        if (payment.userId != token.userId) {
            throw InvalidTokenException("Access denied to this payment record")
        }

        return PaymentMapper.toResult(payment, "Payment details retrieved")
    }

    @Transactional(readOnly = true)
    override fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult> {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        if (token.userId != command.userId) {
            throw InvalidTokenException("Access denied to this user's payment records")
        }

        val payments = paymentRepository.findByUserId(command.userId)
        return PaymentMapper.toResults(payments)
    }
}