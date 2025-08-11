package kr.hhplus.be.server.application.dto.queue.result

data class ExpireQueueTokenResult(
    val tokenId: String,
    val success: Boolean
)