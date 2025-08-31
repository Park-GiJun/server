package kr.hhplus.be.server.application.dto.payment.result

import java.time.LocalDateTime

data class PaymentResult(
    val paymentId: Long,
    val reservationId: Long,
    val userId: String,
    val totalAmount: Int,
    val pointsUsed: Int,
    val actualAmount: Int,
    val paymentAt: LocalDateTime,
    val message: String
)