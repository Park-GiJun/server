package kr.hhplus.be.server.application.handler.event.payment

import kr.hhplus.be.server.application.dto.event.ReservationEventDto
import kr.hhplus.be.server.application.mapper.PaymentEventMapper
import kr.hhplus.be.server.application.port.out.dataplatform.DataPlatformPort
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventHandler(
    private val dataPlatformPort: DataPlatformPort
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
            val reservationEvent = PaymentEventMapper.toReservationEvent(event)
            dataPlatformPort.sendEvent(reservationEvent)
        }
}