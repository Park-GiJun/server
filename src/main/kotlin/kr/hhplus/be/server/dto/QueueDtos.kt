package kr.hhplus.be.server.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class QueueTokenRequest(
    @field:Schema(description = "사용자 ID", example = "user123")
    val userId: String
)

data class QueueTokenStatusRequest(
    @field:Schema(description = "대기열 토큰 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uuid: String,

    @field:Schema(description = "대기 순서", example = "150", minimum = "0")
    val position: Int,

    @field:Schema(description = "유저 ID", example = "user-1")
    val userId: String,

    @field:Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,

    @field:Schema(
        description = "대기 상태",
        example = "WAITING",
        allowableValues = ["WAITING", "ACTIVE", "EXPIRED"]
    )
    val status: QueueTokenStatus,
)

data class QueueTokenResponse(
    @field:Schema(description = "대기열 토큰 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uuid: String,

    @field:Schema(description = "대기 순서", example = "150", minimum = "0")
    val position: Int,

    @field:Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,

    @field:Schema(
        description = "대기 상태",
        example = "WAITING",
        allowableValues = ["WAITING", "ACTIVE", "EXPIRED"]
    )
    val status: QueueTokenStatus,
)