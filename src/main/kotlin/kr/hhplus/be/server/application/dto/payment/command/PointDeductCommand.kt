package kr.hhplus.be.server.application.dto.payment.command

data class PointDeductCommand(
    val sagaId: String,
    val userId: String,
    val amount: Int
)