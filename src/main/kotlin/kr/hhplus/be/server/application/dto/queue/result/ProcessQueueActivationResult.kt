package kr.hhplus.be.server.application.dto.queue.result

data class ProcessQueueActivationResult(
    val concertId: Long,
    val activeTokenCount: Int,
    val tokensToActivate: Int,
    val activatedTokens: List<String>,
    val message: String
)