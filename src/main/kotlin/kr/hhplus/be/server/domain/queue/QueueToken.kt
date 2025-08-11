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
    fun isCompleted(): Boolean = tokenStatus == QueueTokenStatus.COMPLETED

    fun activate(): QueueToken = copy(
        tokenStatus = QueueTokenStatus.ACTIVE,
        updatedAt = LocalDateTime.now()
    )

    fun complete(): QueueToken = copy(
        tokenStatus = QueueTokenStatus.COMPLETED,
        updatedAt = LocalDateTime.now()
    )

    fun expire(): QueueToken = copy(
        tokenStatus = QueueTokenStatus.EXPIRED,
        updatedAt = LocalDateTime.now()
    )

    fun getRedisKey(): String = "queue:token:$queueTokenId"
    fun getUserKey(): String = "queue:user:$userId:$concertId"
    fun getWaitingQueueKey(): String = "queue:waiting:$concertId"
    fun getActiveSetKey(): String = "queue:active:$concertId"
}