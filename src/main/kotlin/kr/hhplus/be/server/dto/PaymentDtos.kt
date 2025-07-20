package kr.hhplus.be.server.dto

import java.time.LocalDateTime

data class PaymentRequest(
    val reservationId: Long,
    val pointsToUse: Int = 0
)

data class PaymentResponse(
    val paymentId: Long,
    val reservationId: Long,
    val totalAmount: Int,
    val pointsUsed: Int,
    val actualAmount: Int,
    val paymentAt: LocalDateTime,
    val message: String
)