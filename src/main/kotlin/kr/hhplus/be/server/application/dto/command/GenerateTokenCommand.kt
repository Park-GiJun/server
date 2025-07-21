package kr.hhplus.be.server.application.dto.command

data class GenerateTokenCommand(
    val userId: String,
    val concertId: Long
)
