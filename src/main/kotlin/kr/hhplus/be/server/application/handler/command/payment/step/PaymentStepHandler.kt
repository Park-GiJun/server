package kr.hhplus.be.server.application.handler.command.payment.step

import kr.hhplus.be.server.application.dto.event.payment.PaymentCreatedEvent
import kr.hhplus.be.server.application.dto.payment.command.CreatePaymentCommand
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.saga.exception.PaymentCreationSagaException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentStepHandler(
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CreatePaymentCommand) {
        try {
            val payment = Payment(
                paymentId = 0L,
                reservationId = command.reservationId,
                userId = command.userId,
                totalAmount = command.totalAmount,
                discountAmount = 0,
                actualAmount = command.pointsUsed
            )

            val savedPayment = paymentRepository.save(payment)

            eventPublisher.publishEvent(
                PaymentCreatedEvent(
                    sagaId = command.sagaId,
                    userId = command.userId,
                    paymentId = savedPayment.paymentId
                )
            )

        } catch (e: Exception) {
            throw PaymentCreationSagaException(
                sagaId = command.sagaId,
                reason = "Payment creation failed: ${e.message}",
                cause = e
            )
        }
    }
}