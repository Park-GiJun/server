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
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

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

    fun isAvailable(): Boolean = seatStatus == SeatStatus.AVAILABLE
    fun isReserved(): Boolean = seatStatus == SeatStatus.RESERVED
    fun isSold(): Boolean = seatStatus == SeatStatus.SOLD

    fun reserve(): ConcertSeat {
        return ConcertSeat(
            concertSeatId = this.concertSeatId,
            concertDateId = this.concertDateId,
            seatNumber = this.seatNumber,
            seatGrade = this.seatGrade,
            seatStatus = SeatStatus.RESERVED
        )
    }

    fun sell(): ConcertSeat {
        return ConcertSeat(
            concertSeatId = this.concertSeatId,
            concertDateId = this.concertDateId,
            seatNumber = this.seatNumber,
            seatGrade = this.seatGrade,
            seatStatus = SeatStatus.SOLD
        )
    }

    fun release(): ConcertSeat {
        return ConcertSeat(
            concertSeatId = this.concertSeatId,
            concertDateId = this.concertDateId,
            seatNumber = this.seatNumber,
            seatGrade = this.seatGrade,
            seatStatus = SeatStatus.AVAILABLE
        )
    }
}

@Repository
class MockConcertSeatGradeRepository {

    private val log = LoggerFactory.getLogger(MockConcertSeatGradeRepository::class.java)
    private val concertSeatGrades = ConcurrentHashMap<Long, ConcertSeatGrade>()

    fun save(concertSeatGrade: ConcertSeatGrade): ConcertSeatGrade {
        concertSeatGrades[concertSeatGrade.concertSeatGradeId] = concertSeatGrade
        log.info("Saved Concert Seat Grade ${concertSeatGrade.concertSeatGradeId}")
        return concertSeatGrade
    }

    fun findBySeatGrade(seatGrade: String, concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGrades.values.filter {
            it.seatGrade == seatGrade && it.concertId == concertId
        }
    }

    fun findByConcertId(concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGrades.values.filter { it.concertId == concertId }
    }

    fun findById(id: Long): ConcertSeatGrade? {
        return concertSeatGrades[id]
    }

    fun findAll(): List<ConcertSeatGrade> {
        return concertSeatGrades.values.toList()
    }

    fun deleteById(id: Long): Boolean {
        return concertSeatGrades.remove(id) != null
    }

    fun clear() {
        concertSeatGrades.clear()
    }
}