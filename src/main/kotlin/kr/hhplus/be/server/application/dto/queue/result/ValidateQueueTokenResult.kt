package kr.hhplus.be.server.application.dto.queue.result

import java.time.LocalDateTime

data class ValidateQueueTokenResult(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val isValid: Boolean,
    val createdAt: LocalDateTime,
    val enteredAt: LocalDateTime
)