package kr.hhplus.be.server.domain.payment

import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.payment.exception.InvalidPaymentAmountException
import kr.hhplus.be.server.domain.payment.exception.PaymentAlreadyProcessedException
import kr.hhplus.be.server.domain.payment.exception.PaymentNotFoundException
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.InvalidReservationStatusException
import kr.hhplus.be.server.domain.reservation.exception.ReservationExpiredException
import kr.hhplus.be.server.domain.users.User
import java.time.LocalDateTime

class PaymentDomainService {

    fun validatePaymentAmount(pointsToUse: Int) {
        if (pointsToUse < 0) {
            throw InvalidPaymentAmountException(pointsToUse)
        }
    }

    fun validateTempReservationForPayment(tempReservation: TempReservation) {
        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(
                tempReservation.status,
                TempReservationStatus.RESERVED
            )
        }

        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(tempReservation.tempReservationId)
        }
    }

    fun validateReservationOwnership(tempReservation: TempReservation, token: QueueToken) {
        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Reservation does not belong to this user")
        }
    }

    fun validatePaymentNotProcessed(existingPayment: Payment?, reservationId: Long) {
        if (existingPayment != null) {
            throw PaymentAlreadyProcessedException(reservationId)
        }
    }

    fun calculatePaymentAmounts(
        seatGrade: ConcertSeatGrade,
        requestedPoints: Int,
        userAvailablePoints: Int
    ): PaymentCalculation {
        val totalAmount = seatGrade.price
        val pointsToUse = minOf(requestedPoints, userAvailablePoints, totalAmount)
        val actualAmount = totalAmount - pointsToUse

        return PaymentCalculation(
            totalAmount = totalAmount,
            pointsToUse = pointsToUse,
            actualAmount = actualAmount
        )
    }

    fun createPayment(
        reservationId: Long,
        userId: String,
        paymentCalculation: PaymentCalculation
    ): Payment {
        return Payment(
            paymentId = 0L,
            reservationId = reservationId,
            userId = userId,
            totalAmount = paymentCalculation.totalAmount,
            discountAmount = paymentCalculation.pointsToUse,
            actualAmount = paymentCalculation.actualAmount,
            paymentAt = LocalDateTime.now(),
            isCancel = false,
            isRefund = false,
            cancelAt = null
        )
    }

    fun validatePaymentExists(payment: Payment?, paymentId: Long) {
        if (payment == null) {
            throw PaymentNotFoundException(paymentId)
        }
    }

    fun validatePaymentAccess(payment: Payment, token: QueueToken) {
        if (payment.userId != token.userId) {
            throw InvalidTokenException("Access denied")
        }
    }

    fun validateUserPaymentAccess(token: QueueToken, requestedUserId: String) {
        if (token.userId != requestedUserId) {
            throw InvalidTokenException("Access denied")
        }
    }

    data class PaymentCalculation(
        val totalAmount: Int,
        val pointsToUse: Int,
        val actualAmount: Int
    )
}