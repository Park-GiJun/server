package kr.hhplus.be.server.application.dto.queue.result

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class GenerateQueueTokenResult(
    val tokenId: String,
    val position: Int,
    val estimatedWaitTime: Int,
    val status: QueueTokenStatus
)