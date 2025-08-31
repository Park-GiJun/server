package kr.hhplus.be.server.infrastructure.adapter.out.event.queue

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class QueueEventMessage(
    val type: String,
    val tokenId: String,
    val concertId: Long,
    val status: String,
    val position: Int,
    val estimatedWaitTime: Int,
    val message: String,
    @param:JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now()
)