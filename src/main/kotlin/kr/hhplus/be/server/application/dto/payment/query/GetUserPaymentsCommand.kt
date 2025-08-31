package kr.hhplus.be.server.application.dto.payment.query


data class GetUserPaymentsCommand(
    val tokenId: String,
    val userId: String
)