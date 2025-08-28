package kr.hhplus.be.server.application.dto.payment.command

data class ProcessPaymentCommand(
    val reservationId: Long,
    val pointsToUse: Int = 0
)
