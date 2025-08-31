package kr.hhplus.be.server.application.dto.event.reservation

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class ReservationConfirmationFailedEvent(
    override val sagaId: String,
    override val userId: String,
    val reservationId: Long,
    val reason: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent