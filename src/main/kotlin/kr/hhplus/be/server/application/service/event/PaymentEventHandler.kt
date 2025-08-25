package kr.hhplus.be.server.application.service.event

import kr.hhplus.be.server.application.dto.event.ReservationEventDto
import kr.hhplus.be.server.application.port.out.dataplatform.DataPlatformPort
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventHandler(
    private val dataPlatformPort: DataPlatformPort
) {
    @Component
    class PaymentEventHandler(
        private val dataPlatformPort: DataPlatformPort
    ) {

        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        fun handlePaymentCompleted(event: PaymentCompletedEvent) {
            val reservationEvent = ReservationEventDto(
                eventType = "COMPLETED",
                reservationId = event.reservationId,
                userId = event.userId,
                concertId = event.concertId,
                seatNumber = event.seatNumber,
                price = event.totalAmount
            )
            dataPlatformPort.sendEvent(reservationEvent)
        }
    }
}