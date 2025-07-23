package kr.hhplus.be.server.infrastructure.adapter.`in`.web.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 충전 요청")
data class PointChargeRequest(
    @field:Schema(description = "충전할 포인트 금액", example = "1000", minimum = "1")
    val amount: Int
)

