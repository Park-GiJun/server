package kr.hhplus.be.server.application.dto.event.point

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class PointDeductionFailedEvent(
    override val sagaId: String,
    override val userId: String,
    val reason: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent
