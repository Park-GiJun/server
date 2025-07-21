package kr.hhplus.be.server.infrastructure.`in`.web.queue.dto

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class GenerateTokenRequest(
    val userId: String
)

data class GenerateTokenResponse(
    val tokenId: String,
    val message: String
)

data class QueueStatusResponse(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val status: QueueTokenStatus,
    val position: Int,
    val estimatedWaitTime: Int
)