package kr.hhplus.be.server.application.dto.event.point

import kr.hhplus.be.server.application.dto.event.saga.SagaEvent
import java.time.LocalDateTime

data class PointRefundedEvent(
    override val sagaId: String,
    override val userId: String,
    val amount: Int,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : SagaEvent