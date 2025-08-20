package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.dto

data class QueueStats(
    val concertId: Long,
    val waitingCount: Long,
    val activeCount: Long
)