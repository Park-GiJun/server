package kr.hhplus.be.server.application.dto.payment

data class ProcessPaymentCommand(
    val reservationId: Long,
    val pointsToUse: Int = 0
)
