package kr.hhplus.be.server.application.dto.queue.result

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class GetQueueStatusResult(
    val tokenId: String,
    val userId: String,
    val status: QueueTokenStatus,
    val concertId: Long,
    val position: Int,
    val estimatedWaitTime: Int
)