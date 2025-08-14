package kr.hhplus.be.server.domain.queue

import java.time.LocalDateTime

data class QueueToken(
    val queueTokenId: String,
    val userId: String,
    val concertId: Long,
    val tokenStatus: QueueTokenStatus,
    val enteredAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isExpired(): Boolean = expiresAt?.isBefore(LocalDateTime.now()) ?: false
    fun isActive(): Boolean = tokenStatus == QueueTokenStatus.ACTIVE && !isExpired()
    fun isWaiting(): Boolean = tokenStatus == QueueTokenStatus.WAITING && !isExpired()

    fun activate(): QueueToken = copy(
        tokenStatus = QueueTokenStatus.ACTIVE,
        updatedAt = LocalDateTime.now()
    )

}