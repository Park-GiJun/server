package kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto

data class PaymentRequest(
    val userId: String,
    val reservationId: Long,
    val seatId: Long,
    val concertDateId: Long,
    val pointsToUse: Int,
    val totalAmount: Int
)

