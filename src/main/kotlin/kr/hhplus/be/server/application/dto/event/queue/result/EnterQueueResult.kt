package kr.hhplus.be.server.application.dto.event.queue.result

data class EnterQueueResult(
    val tokenId: String,
    val position: Int,
    val status: String
)