package kr.hhplus.be.server.application.dto.event.saga.payment

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class PaymentInitiatedEvent(
    override val sagaId: String,
    override val timestamp: LocalDateTime,
    override val userId: String,
    val reservationId: Long,
    val seatId: Long,
    val pointsToUse: Int,
    val totalAmount: Int
) : SagaEvent
