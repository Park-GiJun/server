package kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 충전 요청")
data class UserPointChargeRequest(
    @field:Schema(description = "충전할 포인트 금액", example = "1000", minimum = "1")
    val amount: Int
)

@Schema(description = "포인트 사용 요청")
data class UserPointUseRequest(
    @field:Schema(description = "사용할 포인트 금액", example = "500", minimum = "1")
    val amount: Int
)

@Schema(description = "사용자 포인트 응답")
data class UserPointResponse(
    @field:Schema(description = "사용자 ID", example = "user-1")
    val userId: String,

    @field:Schema(description = "사용자 이름", example = "홍길동")
    val userName: String,

    @field:Schema(description = "총 포인트", example = "10000")
    val totalPoint: Int,

    @field:Schema(description = "사용 가능한 포인트", example = "8500")
    val availablePoint: Int,

    @field:Schema(description = "사용한 포인트", example = "1500")
    val usedPoint: Int
)
