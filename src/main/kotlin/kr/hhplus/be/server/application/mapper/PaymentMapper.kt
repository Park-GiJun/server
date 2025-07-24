package kr.hhplus.be.server.application.mapper

import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.domain.payment.Payment

object PaymentMapper {

    fun toResult(domain: Payment, message: String = "Payment processed successfully"): PaymentResult {
        return PaymentResult(
            paymentId = domain.paymentId,
            reservationId = domain.reservationId,
            userId = domain.userId,
            totalAmount = domain.totalAmount,
            pointsUsed = domain.discountAmount,
            actualAmount = domain.actualAmount,
            paymentAt = domain.paymentAt,
            message = message
        )
    }

    fun toResults(domains: List<Payment>): List<PaymentResult> {
        return domains.map { toResult(it, "Payment record") }
    }
}