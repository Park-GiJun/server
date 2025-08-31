package kr.hhplus.be.server.application.dto.event.concert

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class SeatConfirmedEvent(
    override val sagaId: String,
    override val userId: String,
    val seatId: Long,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent