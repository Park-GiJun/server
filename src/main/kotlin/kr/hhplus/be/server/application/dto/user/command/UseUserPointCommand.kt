package kr.hhplus.be.server.application.dto.user.command

data class UseUserPointCommand(
    val userId: String,
    val amount: Int
)