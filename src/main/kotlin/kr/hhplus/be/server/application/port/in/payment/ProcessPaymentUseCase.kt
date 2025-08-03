package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand

interface ProcessPaymentUseCase {
    fun processPayment(command: ProcessPaymentCommand): PaymentResult
}