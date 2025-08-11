package kr.hhplus.be.server.application.dto.queue.result

data class ProcessQueueActivationResult(
    val concertId: Long,
    val activatedTokenIds: List<String>,
    val activatedCount: Int,
    val remainingWaitingCount: Long
)