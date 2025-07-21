package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.log.PointHistory
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.dto.PaymentRequest
import kr.hhplus.be.server.exception.*
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.MockConcertSeatGradeRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.MockConcertSeatRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.MockUserRepository
import kr.hhplus.be.server.repository.mock.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentService(
    private val paymentRepository: MockPaymentRepository,
    private val tempReservationRepository: MockTempReservationRepository,
    private val userRepository: MockUserRepository,
    private val concertSeatRepository: MockConcertSeatRepository,
    private val reservationRepository: MockReservationRepository,
    private val pointHistoryRepository: MockPointHistoryRepository,
    private val concertSeatGradeRepository: MockConcertSeatGradeRepository,
    private val queueService: QueueService
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    @Transactional
    fun processPayment(tokenId: String, request: PaymentRequest): Payment {
        log.info("Processing payment for reservation: ${request.reservationId}")

        val token = queueService.validateActiveToken(tokenId)

        val tempReservation = tempReservationRepository.findByTempReservationId(request.reservationId)
            ?: throw QueueTokenNotFoundException("Temporary reservation not found")

        if (!tempReservation.isReserved()) {
            throw InvalidTokenStatusException("Reservation is not in reserved status")
        }

        if (tempReservation.isExpired()) {
            throw ConcertDateExpiredException("Reservation has expired")
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Reservation does not belong to this user")
        }

        val existingPayment = paymentRepository.findByReservationId(request.reservationId)
        if (existingPayment != null) {
            throw IllegalStateException("Payment already processed for this reservation")
        }

        val user = userRepository.findByUserId(token.userId)
            ?: throw UserNotFoundException("User not found: ${token.userId}")

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertNotFoundException("Seat not found")

        val concertDate = concertSeatRepository.findConcertSeats(seat.concertDateId)
            ?.firstOrNull()?.let {
                seat.concertDateId
            }

        val seatGradeList = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, token.concertId)
        val seatGrade = seatGradeList.firstOrNull()
            ?: throw ConcertNotFoundException("Seat grade not found")

        val totalAmount = seatGrade.price
        val pointsToUse = minOf(request.pointsToUse, user.availablePoint, totalAmount)
        val actualAmount = totalAmount - pointsToUse

        if (user.availablePoint < actualAmount) {
            throw InsufficientPointException("Insufficient balance. Required: $actualAmount, Available: ${user.availablePoint}")
        }

        user.availablePoint -= actualAmount
        user.usedPoint += pointsToUse
        userRepository.save(user)

        val payment = Payment(
            paymentId = 0L,
            reservationId = request.reservationId,
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

        val reservation = kr.hhplus.be.server.domain.reservation.Reservation(
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

        queueService.completeToken(tokenId)

        log.info("Payment completed successfully. PaymentId: ${savedPayment.paymentId}")
        return savedPayment
    }

    fun getPayment(tokenId: String, paymentId: Long): Payment {
        val token = queueService.validateActiveToken(tokenId)

        val payment = paymentRepository.findByPaymentId(paymentId)
            ?: throw IllegalArgumentException("Payment not found: $paymentId")

        if (payment.userId != token.userId) {
            throw InvalidTokenException("Access denied to this payment record")
        }

        return payment
    }

    fun getUserPayments(tokenId: String, userId: String): List<Payment> {
        val token = queueService.validateActiveToken(tokenId)

        if (token.userId != userId) {
            throw InvalidTokenException("Access denied to this user's payment records")
        }

        return paymentRepository.findByUserId(userId)
    }
}