package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "queue_token")
class QueueTokenJpaEntity(
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
) : BaseEntity()