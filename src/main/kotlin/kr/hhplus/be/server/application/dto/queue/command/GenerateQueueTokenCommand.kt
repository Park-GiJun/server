package kr.hhplus.be.server.application.dto.queue.command

data class GenerateQueueTokenCommand(
    val userId: String,
    val concertId: Long
)