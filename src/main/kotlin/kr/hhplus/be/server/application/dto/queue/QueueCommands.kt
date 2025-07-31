package kr.hhplus.be.server.application.dto.queue

import kr.hhplus.be.server.domain.queue.QueueToken

data class ActivateTokensCommand(
    val concertId: Long,
    val count: Int = 3
)

data class CompleteTokenCommand(
    val tokenId: String
)

data class ExpireTokenCommand(
    val tokenId: String
)

data class GenerateTokenCommand(
    val userId: String,
    val concertId: Long
)

data class ValidateTokenCommand(
    val tokenId: String,
    val concertId: Long? = null
)

data class QueuePositionChange(
    val token: QueueToken,
    val oldPosition: Int,
    val newPosition: Int
)

data class UpdateQueuePositionsCommand(
    val concertId: Long
)

