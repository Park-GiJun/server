package kr.hhplus.be.server.application.dto.queue.command

data class GenerateTokenCommand(
    val userId: String,
    val concertId: Long
)
