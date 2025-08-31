package kr.hhplus.be.server.domain.payment.event

import java.time.LocalDateTime

data class PaymentCompletedEvent(
    val paymentId: Long,
    val reservationId: Long,
    val userId: String,
    val concertId: Long,
    val seatNumber: String,
    val totalAmount: Int,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)