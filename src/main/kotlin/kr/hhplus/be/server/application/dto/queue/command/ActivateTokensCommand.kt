package kr.hhplus.be.server.application.dto.queue.command

data class ActivateTokensCommand(
    val concertId: Long,
    val count: Int = 10
)