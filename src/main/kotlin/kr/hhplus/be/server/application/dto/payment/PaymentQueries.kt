package kr.hhplus.be.server.application.dto.payment


data class GetPaymentCommand(
    val tokenId: String,
    val paymentId: Long
)

data class GetUserPaymentsCommand(
    val tokenId: String,
    val userId: String
)