package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.result.PaymentResult
import kr.hhplus.be.server.application.dto.payment.command.ProcessPaymentCommand

interface ProcessPaymentUseCase {
    fun processPayment(command: ProcessPaymentCommand): PaymentResult
}