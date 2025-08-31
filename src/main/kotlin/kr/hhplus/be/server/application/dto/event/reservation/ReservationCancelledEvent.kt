package kr.hhplus.be.server.application.dto.event.reservation

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class ReservationCancelledEvent(
    override val sagaId: String,
    override val userId: String,
    val reservationId: Long,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent