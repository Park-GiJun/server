package kr.hhplus.be.server.application.dto.queue

data class GetQueueStatusQuery(
    val tokenId: String
)

data class GetActivateTokensCountQuery(
    val concertId: Long
)