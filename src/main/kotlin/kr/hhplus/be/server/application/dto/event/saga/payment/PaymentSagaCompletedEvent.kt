package kr.hhplus.be.server.application.dto.event.saga.payment

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class PaymentSagaCompletedEvent(
    override val sagaId: String,
    override val userId: String,
    val paymentId: Long,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent