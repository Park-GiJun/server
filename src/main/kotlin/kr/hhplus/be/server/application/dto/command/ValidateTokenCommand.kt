package kr.hhplus.be.server.application.dto.command

data class ValidateTokenCommand(
    val tokenId: String,
    val concertId: Long? = null
)