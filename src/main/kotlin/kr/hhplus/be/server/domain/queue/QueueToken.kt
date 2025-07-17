package kr.hhplus.be.server.domain.queue

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity
import kr.hhplus.be.server.dto.QueueTokenResponse
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "queue_token")
class QueueToken(
    @Id
    @Column(name = "queue_token_id")
    val queueToken: String = UUID.randomUUID().toString(),

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "concert_id")
    val concertId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status")
    val tokenStatus: QueueTokenStatus,

    @Column(name = "entered_at")
    val enteredAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity() {

    fun isExpired(): Boolean = tokenStatus == QueueTokenStatus.EXPIRED

    fun isActive(): Boolean = tokenStatus == QueueTokenStatus.ACTIVE

    fun isWaiting(): Boolean = tokenStatus == QueueTokenStatus.WAITING

    fun activate(): QueueToken {
        return QueueToken(
            queueToken = this.queueToken,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.ACTIVE,
            enteredAt = this.enteredAt
        )
    }

    fun expire(): QueueToken {
        return QueueToken(
            queueToken = this.queueToken,
            userId = this.userId,
            concertId = this.concertId,
            tokenStatus = QueueTokenStatus.EXPIRED,
            enteredAt = this.enteredAt
        )
    }
}