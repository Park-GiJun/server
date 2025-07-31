package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand
import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.CompleteTokenUseCase
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
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
) : ProcessPaymentUseCase {

    private val paymentDomainService = PaymentDomainService()
    private val userDomainService = UserDomainService()

    override fun processPayment(command: ProcessPaymentCommand): PaymentResult {
        paymentDomainService.validatePaymentAmount(command.pointsToUse)

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )
        val token = createQueueTokenFromResult(tokenResult)

        val tempReservation = tempReservationRepository.findByTempReservationId(command.reservationId)
            ?: throw TempReservationNotFoundException(command.reservationId)

        paymentDomainService.validateTempReservationForPayment(tempReservation)
        paymentDomainService.validateReservationOwnership(tempReservation, token)

        val existingPayment = paymentRepository.findByReservationId(command.reservationId)
        paymentDomainService.validatePaymentNotProcessed(existingPayment, command.reservationId)

        val user = userRepository.findByUserId(token.userId)
        userDomainService.validateUserExists(user, token.userId)

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val seatGrades = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, token.concertId)
        val seatGrade = seatGrades.firstOrNull()
            ?: throw ConcertNotFoundException(token.concertId)

        val paymentCalculation = paymentDomainService.calculatePaymentAmounts(
            seatGrade, command.pointsToUse, user!!.availablePoint
        )

        userDomainService.validateSufficientBalance(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )

        val updatedUser = userDomainService.useUserPoint(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )
        userRepository.save(updatedUser)

        val payment = paymentDomainService.createPayment(
            command.reservationId, token.userId, paymentCalculation
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
            paymentAmount = paymentCalculation.actualAmount
        )
        reservationRepository.save(reservation)

        if (paymentCalculation.pointsToUse > 0) {
            val pointHistory = PointHistory(
                pointHistoryId = 0L,
                userId = token.userId,
                pointHistoryType = "USED",
                pointHistoryAmount = paymentCalculation.pointsToUse,
                description = "Concert ticket payment"
            )
            pointHistoryRepository.save(pointHistory)
        }

        completeTokenUseCase.completeToken(CompleteTokenCommand(command.tokenId))

        return PaymentMapper.toResult(savedPayment, "Payment completed successfully")
    }

    private fun createQueueTokenFromResult(tokenResult: ValidateTokenResult): QueueToken {
        return QueueToken(
            queueTokenId = tokenResult.tokenId,
            userId = tokenResult.userId,
            concertId = tokenResult.concertId,
            tokenStatus = QueueTokenStatus.ACTIVE
        )
    }
}