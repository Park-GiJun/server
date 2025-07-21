package kr.hhplus.be.server.application.dto.queue.command

data class ValidateTokenCommand(
    val tokenId: String,
    val concertId: Long? = null
)