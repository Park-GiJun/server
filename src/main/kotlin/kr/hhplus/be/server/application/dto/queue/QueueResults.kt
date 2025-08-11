package kr.hhplus.be.server.application.dto.queue

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class GenerateQueueTokenResult(
    val tokenId: String,
    val position: Int,
    val estimatedWaitTime: Int,
    val status: QueueTokenStatus
)

data class ValidateQueueTokenResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val isValid: Boolean
)

data class ExpireQueueTokenResult(
    val success: Boolean
)

data class CompleteQueueTokenResult(
    val success: Boolean
)

data class GetQueueStatusResult(
    val tokenId: String,
    val userId: String,
    val status: QueueTokenStatus,
    val concertId: Long,
    val position: Int,
    val estimatedWaitTime: Int
)

data class GetActivateTokensCountResult(
    val concertId: Long,
    val activeCount: Int
)

data class ProcessQueueActivationResult(
    val concertId: Long,
    val activeTokenCount: Int,
    val tokensToActivate: Int,
    val activatedTokens: List<String>,
    val message: String
)