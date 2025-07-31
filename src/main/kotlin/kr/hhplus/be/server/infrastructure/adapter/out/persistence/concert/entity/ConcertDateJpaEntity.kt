package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity

import jakarta.persistence.*
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(
    name = "concert_date",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UKCONCERTDATEX0",
            columnNames = ["concert_Id", "concert_session"]
        )
    ],
    indexes = [
        Index(name = "CONCERTDATEX0", columnList = "concert_id"),
        Index(name = "CONCERTDATEX1", columnList = "date"),
        Index(name = "CONCERTDATEX2", columnList = "is_sold_out"),
    Index(name = "CONCERTDATEX3", columnList = "date,concert_id")
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