package kr.hhplus.be.server.application.dto.queue.result

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class QueueStatusResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val status: QueueTokenStatus,
    val position: Int,
)