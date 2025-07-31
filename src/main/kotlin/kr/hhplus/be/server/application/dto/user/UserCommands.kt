package kr.hhplus.be.server.application.dto.user

data class ChargeUserPointCommand(
    val userId: String,
    val amount: Int
)

data class UseUserPointCommand(
    val userId: String,
    val amount: Int
)