package kr.hhplus.be.server.application.dto.event.saga

import java.time.LocalDateTime

interface SagaEvent {
    val sagaId: String
    val timestamp: LocalDateTime
    val userId: String
}