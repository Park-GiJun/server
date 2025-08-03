package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult

interface GetUserPaymentsUseCase {
    fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult>
}