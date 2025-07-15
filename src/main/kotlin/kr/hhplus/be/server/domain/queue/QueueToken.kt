package kr.hhplus.be.server.domain.queue

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "queue_token")
@Entity
class QueueToken(
    @Id
    @Column("queue_token_id")
    val queueToken : String = UUID.randomUUID().toString(),

    @Column("user_id")
    val userId : String,

    @Column("concert_id")
    val concertId : Long,

    @Column("token_status")
    val tokenStatus : String,

    @Column("entered_at")
    val enteredAt : LocalDateTime = LocalDateTime.now()
) : BaseEntity() {
}