package kr.hhplus.be.server.application.port.out.event.payment

import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent

interface PaymentEventPort {
    fun publishPaymentCompleted(event: PaymentCompletedEvent)
}