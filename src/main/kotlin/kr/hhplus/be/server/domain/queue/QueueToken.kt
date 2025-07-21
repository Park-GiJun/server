package kr.hhplus.be.server.domain.queue

import java.time.LocalDateTime
import java.util.*

class QueueToken(
    val queueTokenId: String = UUID.randomUUID().toString(),
    val userId: String,
    val concertId: Long,
    var tokenStatus: QueueTokenStatus,
    val enteredAt: LocalDateTime = LocalDateTime.now(),

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
) {

    fun isExpired(): Boolean = tokenStatus == QueueTokenStatus.EXPIRED

    fun isActive(): Boolean = tokenStatus == QueueTokenStatus.ACTIVE && !isExpired()

    fun isWaiting(): Boolean = tokenStatus == QueueTokenStatus.WAITING && !isExpired()

    fun activate(): QueueToken {
        return QueueToken(
            queueTokenId = this.queueTokenId,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.ACTIVE,
            enteredAt = this.enteredAt,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun expire(): QueueToken {
        return QueueToken(
            queueTokenId = this.queueTokenId,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.EXPIRED,
            enteredAt = this.enteredAt,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun cancel(): QueueToken {
        return QueueToken(
            queueTokenId = this.queueTokenId,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.CANCELLED,
            enteredAt = this.enteredAt,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun complete(): QueueToken {
        return QueueToken(
            queueTokenId = this.queueTokenId,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.COMPLETED,
            enteredAt = this.enteredAt,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }
}