package kr.hhplus.be.server.application.dto.command

data class ActivateTokensCommand(
    val concertId: Long,
    val count: Int = 10
)