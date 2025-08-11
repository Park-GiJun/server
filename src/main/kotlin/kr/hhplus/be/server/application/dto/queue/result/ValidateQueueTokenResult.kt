package kr.hhplus.be.server.application.dto.queue.result

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class ValidateQueueTokenResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val isValid: Boolean,
    val status: QueueTokenStatus
)