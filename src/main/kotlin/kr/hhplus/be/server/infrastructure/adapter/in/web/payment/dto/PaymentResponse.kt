package kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto

data class PaymentResponse(
    val sagaId: String,
    val paymentId: Long? = null,
    val status: String,
    val message: String? = null
)