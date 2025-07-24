package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

@Entity
@Table(
    name = "concert_seat",
    indexes = [
        Index(name = "idx_concert_date_id", columnList = "concert_date_id"),
        Index(name = "idx_seat_status", columnList = "seat_status")
    ]
)
class ConcertSeatJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_seat_id", nullable = false)
    val concertSeatId: Long = 0L,

    @Column(name = "concert_date_id", nullable = false)
    val concertDateId: Long,

    @Column(name = "seat_number", nullable = false, length = 10)
    val seatNumber: String,

    @Column(name = "seat_grade", nullable = false, length = 20)
    val seatGrade: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false)
    val seatStatus: SeatStatus = SeatStatus.AVAILABLE
) : BaseEntity() {

}