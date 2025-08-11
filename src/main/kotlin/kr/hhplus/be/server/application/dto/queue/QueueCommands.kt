package kr.hhplus.be.server.application.dto.queue

data class GenerateQueueTokenCommand(
    val userId: String,
    val concertId: Long
)

data class ValidateQueueTokenCommand(
    val tokenId: String,
    val concertId: Long? = null
)

data class ExpireQueueTokenCommand(
    val tokenId: String
)

data class CompleteQueueTokenCommand(
    val tokenId: String
)

data class ProcessQueueActivationCommand(
    val concertId: Long
)