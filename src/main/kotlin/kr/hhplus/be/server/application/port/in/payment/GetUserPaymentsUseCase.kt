package kr.hhplus.be.server.application.port.`in`.payment

import kr.hhplus.be.server.application.dto.payment.query.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.result.PaymentResult

interface GetUserPaymentsUseCase {
    fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult>
}