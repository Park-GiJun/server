package kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.entity

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.time.LocalDateTime

@RedisHash("queue:token")
data class QueueTokenRedisEntity(
    @Id
    val queueTokenId: String,
    val userId: String,
    val concertId: Long,
    val tokenStatus: String,
    val enteredAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun fromDomain(queueToken: QueueToken): QueueTokenRedisEntity {
            return QueueTokenRedisEntity(
                queueTokenId = queueToken.queueTokenId,
                userId = queueToken.userId,
                concertId = queueToken.concertId,
                tokenStatus = queueToken.tokenStatus.name,
                enteredAt = queueToken.enteredAt,
                expiresAt = queueToken.expiresAt,
                updatedAt = LocalDateTime.now()
            )
        }
    }

    fun toDomain(): QueueToken {
        return QueueToken(
            queueTokenId = queueTokenId,
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.valueOf(tokenStatus),
            enteredAt = enteredAt,
            expiresAt = expiresAt
        )
    }
}