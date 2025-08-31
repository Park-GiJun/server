package kr.hhplus.be.server.application.dto.event.saga.payment

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import java.time.LocalDateTime

data class PaymentSagaFailedEvent(
    override val sagaId: String,
    override val userId: String,
    val failureReason: String,
    val failedAt: LocalDateTime = LocalDateTime.now(),
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent