package kr.hhplus.be.server.application.dto.event.reservation

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class ReservationConfirmedEvent(
    override val sagaId: String,
    override val userId: String,
    val reservationId: Long,
    val actualReservationId: Long,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent