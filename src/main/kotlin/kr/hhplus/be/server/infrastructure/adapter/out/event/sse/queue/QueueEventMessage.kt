package kr.hhplus.be.server.infrastructure.adapter.out.event.sse.queue

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class QueueEventMessage(
    val type: String,              // QUEUE_ENTERED, POSITION_UPDATED, TOKEN_ACTIVATED, TOKEN_EXPIRED
    val tokenId: String,
    val concertId: Long,
    val status: String,            // WAITING, ACTIVE, EXPIRED
    val position: Int,
    val estimatedWaitTime: Int,    // 분 단위
    val message: String,
    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now()
)