package kr.hhplus.be.server.domain.queue

import jakarta.persistence.*
import kr.hhplus.be.server.domain.BaseEntity
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "queue_token")
class QueueToken(
    @Id
    @Column(name = "queue_token_id")
    val queueTokenId: String = UUID.randomUUID().toString(),

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "concert_id")
    val concertId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status")
    var tokenStatus: QueueTokenStatus,

    @Column(name = "entered_at")
    val enteredAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime = LocalDateTime.now().plusHours(1)
) : BaseEntity() {

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isActive(): Boolean = tokenStatus == QueueTokenStatus.ACTIVE && !isExpired()

    fun isWaiting(): Boolean = tokenStatus == QueueTokenStatus.WAITING && !isExpired()

    fun activate(): QueueToken {
        this.tokenStatus = QueueTokenStatus.ACTIVE
        this.expiresAt = LocalDateTime.now().plusMinutes(30) // 활성화 후 30분
        return this
    }

    fun expire(): QueueToken {
        this.tokenStatus = QueueTokenStatus.EXPIRED
        return this
    }

    fun cancel(): QueueToken {
        this.tokenStatus = QueueTokenStatus.CANCELLED
        return this
    }

    fun complete(): QueueToken {
        this.tokenStatus = QueueTokenStatus.COMPLETED
        return this
    }
}