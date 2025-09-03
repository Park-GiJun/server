package kr.hhplus.be.server.application.dto.event.queue.result

data class GetQueueStatusKafkaResult(
    val tokenId: String,
    val position: Int,
    val activeCount: Int,
    val waitingCount: Int,
    val status: String
)