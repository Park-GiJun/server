package kr.hhplus.be.server.application.port.out.payment

import kr.hhplus.be.server.domain.payment.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findByPaymentId(paymentId: Long): Payment?
    fun findByReservationId(reservationId: Long): Payment?
    fun findByUserId(userId: String): List<Payment>
}
