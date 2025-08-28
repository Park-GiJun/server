package kr.hhplus.be.server.application.dto.reservation.command

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class ReservationCancelCommand (
    override val sagaId: String,
    override val userId: String,
    val reservationId: Long,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent