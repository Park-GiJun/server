package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.query.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.result.PaymentResult

interface GetPaymentUseCase {
    fun getPayment(command: GetPaymentCommand): PaymentResult
}