package kr.hhplus.be.server.application.dto.queue.command

data class ExpireQueueTokenCommand(
    val tokenId: String,
    val reason: String = "토큰이 만료되었습니다"
)
