package kr.hhplus.be.server.application.dto.event.queue.query

data class GetQueueStatusKafkaQuery(
    val tokenId: String,
    val concertId: Long
)