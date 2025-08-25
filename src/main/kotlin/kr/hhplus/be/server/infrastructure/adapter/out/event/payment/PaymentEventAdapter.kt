package kr.hhplus.be.server.infrastructure.adapter.out.event.payment

import kr.hhplus.be.server.application.port.out.event.payment.PaymentEventPort
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class PaymentEventAdapter(
    private val applicationEventPublisher: ApplicationEventPublisher
) : PaymentEventPort {

    override fun publishPaymentCompleted(event: PaymentCompletedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}