package kr.hhplus.be.server.application.dto.queue.result

data class ActivateTokensResult(
    val activatedCount: Int,
    val tokenIds: List<String>
)