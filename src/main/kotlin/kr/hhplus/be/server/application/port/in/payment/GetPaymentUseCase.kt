package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult

interface GetPaymentUseCase {
    fun getPayment(command: GetPaymentCommand): PaymentResult
}