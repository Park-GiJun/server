package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.ProcessPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.queue.CompleteQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.queue.service.QueueTokenDomainService
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.lock.DistributedLockType
import org.slf4j.LoggerFactory
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
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val completeTokenUseCase: CompleteQueueTokenUseCase,
    private val paymentDomainService: PaymentDomainService,
    private val userDomainService: UserDomainService,
    private val concertDateRepository: ConcertDateRepository
) : ProcessPaymentUseCase {
    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "lock:payment:user:#{#command.userId}",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun processPayment(command: ProcessPaymentCommand): PaymentResult {
        paymentDomainService.validatePaymentAmount(command.pointsToUse)
        val tempReservation = tempReservationRepository.findByTempReservationId(command.reservationId)
            ?: throw TempReservationNotFoundException(command.reservationId)
        paymentDomainService.validateTempReservationForPayment(tempReservation)
        val existingPayment = paymentRepository.findByReservationId(command.reservationId)
        paymentDomainService.validatePaymentNotProcessed(existingPayment, command.reservationId)
        val user = userRepository.findByUserId(tempReservation.userId)
        userDomainService.validateUserExists(user, tempReservation.userId)
        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)
        val concert = concertDateRepository.findByConcertDateId(seat.concertDateId)
            ?: throw ConcertNotFoundException(seat.concertDateId)
        val seatGrades = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, concert.concertId)
        val seatGrade = seatGrades.firstOrNull()
            ?: throw ConcertNotFoundException(concert.concertId)
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
            command.reservationId, tempReservation.userId, paymentCalculation
        )
        val savedPayment = paymentRepository.save(payment)
        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)
        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)
        val reservation = Reservation(
            reservationId = 0L,
            userId = tempReservation.userId,
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
                userId = tempReservation.userId,
                pointHistoryType = "USED",
                pointHistoryAmount = paymentCalculation.pointsToUse,
                description = "Concert ticket payment"
            )
            pointHistoryRepository.save(pointHistory)
        }
        return PaymentMapper.toResult(savedPayment, "Payment completed successfully")
    }
}