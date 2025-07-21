package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity

import jakarta.persistence.*
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(
    name = "concert_dates",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_concert_date_session",
            columnNames = ["concert_Id", "concert_session"]
        )
    ],
    indexes = [
        Index(name = "idx_concert", columnList = "concert_id"),
        Index(name = "idx_date", columnList = "date"),
        Index(name = "idx_available_seats", columnList = "available_seats")
    ]
)
class ConcertDateJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_date_id", nullable = false)
    val concertDateId: Long = 0L,

    @Column(name = "concert_session", nullable = false)
    val concertSession: Long,

    @Column(name = "concert_id", nullable = false)
    val concertId: Long,

    @Column(name = "date", nullable = false)
    val date: LocalDateTime,

    @Column(name = "is_sold_out", nullable = false)
    val isSoldOut: Boolean = false
) : BaseEntity()