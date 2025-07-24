package kr.hhplus.be.server.application.dto.user

data class GetUserCommand(
    val userId: String
)

data class ChargeUserPointCommand(
    val userId: String,
    val amount: Int
)

data class UseUserPointCommand(
    val userId: String,
    val amount: Int
)