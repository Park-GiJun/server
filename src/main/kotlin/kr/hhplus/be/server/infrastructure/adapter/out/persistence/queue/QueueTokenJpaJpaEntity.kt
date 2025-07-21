package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue

import jakarta.persistence.*
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "queue_token")
class QueueTokenJpaJpaEntity(
    @Id
    @Column(name = "queue_token_id")
    val queueTokenId: String,

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "concert_id")
    val concertId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status")
    var tokenStatus: QueueTokenStatus,

    @Column(name = "entered_at")
    val enteredAt: LocalDateTime,
) : BaseEntity() {

    fun toDomain(): QueueToken {
        return QueueToken(
            queueTokenId = this.queueTokenId,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = this.tokenStatus,
            enteredAt = this.enteredAt,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    companion object {
        fun fromDomain(domain: QueueToken): QueueTokenJpaJpaEntity {
            return QueueTokenJpaJpaEntity(
                queueTokenId = domain.queueTokenId,
                userId = domain.userId,
                concertId = domain.concertId,
                tokenStatus = domain.tokenStatus,
                enteredAt = domain.enteredAt,
            ).apply {
            }
        }
    }
}