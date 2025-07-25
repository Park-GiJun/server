package kr.hhplus.be.server.domain.concert

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity

@Entity
@Table(name = "concert_seat_grade")
class ConcertSeatGrade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column("concert_seat_grade_id", nullable = false)
    val concertSeatGradeId: Long = 0L,

    @Column(name = "concert_id")
    val concertId: Long,

    @Column(name = "seat_grade")
    val seatGrade: String,

    @Column(name = "price")
    val price: Int
) : BaseEntity() {
}