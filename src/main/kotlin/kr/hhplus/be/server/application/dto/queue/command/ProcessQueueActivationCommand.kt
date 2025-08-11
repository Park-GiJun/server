package kr.hhplus.be.server.application.dto.queue.command

data class ProcessQueueActivationCommand(
    val concertId: Long,
    val maxActiveTokens: Int = 100
)