package kr.hhplus.be.server.application.dto.event.queue.command

data class EnterQueueCommand(
    val userId: Long,
    val concertId: Long
)