package kr.hhplus.be.server.application.dto.queue

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class ActivateTokensResult(
    val activatedCount: Int,
    val tokenIds: List<String>
)

data class QueueStatusResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val status: QueueTokenStatus,
    val position: Int,
)

data class ValidateTokenResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val isValid: Boolean
)

data class UpdateQueuePositionsResult(
    val concertId: Long,
    val updatedCount: Int,
    val positionChanges: List<QueuePositionChange>
)


data class ProcessQueueActivationResult(
    val concertId: Long,
    val activeTokenCount: Int,
    val tokensToActivate: Int,
    val activatedTokens: List<String>,
    val message: String
)