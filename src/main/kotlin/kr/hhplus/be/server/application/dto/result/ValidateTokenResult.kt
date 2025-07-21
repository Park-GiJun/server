package kr.hhplus.be.server.application.dto.result

data class ValidateTokenResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val isValid: Boolean
)