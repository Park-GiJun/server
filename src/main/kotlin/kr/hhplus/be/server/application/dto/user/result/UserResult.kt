package kr.hhplus.be.server.application.dto.user.result

data class UserResult(
    val userId: String,
    val userName: String,
    val totalPoint: Int,
    val availablePoint: Int,
    val usedPoint: Int
)
