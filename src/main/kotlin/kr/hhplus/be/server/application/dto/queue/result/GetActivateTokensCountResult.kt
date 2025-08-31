package kr.hhplus.be.server.application.dto.queue.result

data class GetActivateTokensCountResult(
    val concertId: Long,
    val activeCount: Int
)