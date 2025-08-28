package kr.hhplus.be.server.application.dto.user.command

data class ChargeUserPointCommand(
    val userId: String,
    val amount: Int
)