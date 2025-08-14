package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto

data class GenerateTokenRequest(
    val userId: String
)

data class GenerateTokenResponse(
    val tokenId: String,
    val position: Int,
    val estimatedWaitTime: Int,
    val status: String,
    val message: String
)

data class QueueStatusResponse(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val status: String,
    val position: Int,
    val estimatedWaitTime: Int,
    val message: String
)