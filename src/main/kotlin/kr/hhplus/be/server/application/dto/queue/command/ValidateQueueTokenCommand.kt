package kr.hhplus.be.server.application.dto.queue.command

data class ValidateQueueTokenCommand(
    val tokenId: String,
    val concertId: Long
)
