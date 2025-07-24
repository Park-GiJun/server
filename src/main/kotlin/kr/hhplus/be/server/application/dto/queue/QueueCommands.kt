package kr.hhplus.be.server.application.dto.queue

data class ActivateTokensCommand(
    val concertId: Long,
    val count: Int = 10
)

data class CompleteTokenCommand(
    val tokenId: String
)

data class ExpireTokenCommand(
    val tokenId: String
)

data class GenerateTokenCommand(
    val userId: String,
    val concertId: Long
)

data class ValidateTokenCommand(
    val tokenId: String,
    val concertId: Long? = null
)