package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand

interface ProcessPaymentUseCase {
    fun processPayment(command: ProcessPaymentCommand): PaymentResult
}

interface GetPaymentUseCase {
    fun getPayment(command: GetPaymentCommand): PaymentResult
}

interface GetUserPaymentsUseCase {
    fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult>
}