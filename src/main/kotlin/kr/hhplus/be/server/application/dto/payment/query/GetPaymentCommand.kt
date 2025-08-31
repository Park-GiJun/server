package kr.hhplus.be.server.application.dto.payment.query

data class GetPaymentCommand(
    val tokenId: String,
    val paymentId: Long
)