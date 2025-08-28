package kr.hhplus.be.server.application.dto.payment.command

data class CreatePaymentCommand(
    val sagaId: String,
    val reservationId: Long,
    val userId: String,
    val totalAmount: Int,
    val pointsUsed: Int
)