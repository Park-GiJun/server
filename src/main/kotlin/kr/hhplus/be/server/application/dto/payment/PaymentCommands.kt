package kr.hhplus.be.server.application.dto.payment

data class ProcessPaymentCommand(
    val tokenId: String,
    val reservationId: Long,
    val pointsToUse: Int = 0
)
