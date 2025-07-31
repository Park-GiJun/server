package kr.hhplus.be.server._disable_mock.payment.mock

import kr.hhplus.be.server.domain.payment.Payment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MockPaymentRepository {
    private val log = LoggerFactory.getLogger(MockPaymentRepository::class.java)
    private val payments = ConcurrentHashMap<Long, Payment>()
    private val idGenerator = AtomicLong(1)

    fun save(payment: Payment): Payment {
        val newPayment = if (payment.paymentId == 0L) {
            Payment(
                paymentId = idGenerator.getAndIncrement(),
                reservationId = payment.reservationId,
                userId = payment.userId,
                totalAmount = payment.totalAmount,
                discountAmount = payment.discountAmount,
                actualAmount = payment.actualAmount,
                paymentAt = payment.paymentAt,
                isCancel = payment.isCancel,
                isRefund = payment.isRefund,
                cancelAt = payment.cancelAt
            )
        } else {
            payment
        }

        payments[newPayment.paymentId] = newPayment
        log.info("Saved payment: ${newPayment.paymentId}")
        return newPayment
    }

    fun findByPaymentId(paymentId: Long): Payment? {
        log.info("Finding payment, paymentId: $paymentId")
        return payments[paymentId]
    }

    fun findByReservationId(reservationId: Long): Payment? {
        log.info("Finding payment by reservation, reservationId: $reservationId")
        return payments.values.find { it.reservationId == reservationId }
    }

    fun findByUserId(userId: String): List<Payment> {
        log.info("Finding payments by user, userId: $userId")
        return payments.values.filter { it.userId == userId }.sortedByDescending { it.paymentAt }
    }
}