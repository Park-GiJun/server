package kr.hhplus.be.server.domain.concert

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity

@Entity
@Table(
    name = "concert_seat",
    indexes = [
        Index(name = "idx_concert_date_id", columnList = "concert_date_id"),
        Index(name = "idx_seat_status", columnList = "seat_status")
    ]
)
class ConcertSeat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_seat_id", nullable = false)
    val concertSeatId: Long = 0,

    @Column(name = "concert_date_id", nullable = false)
    val concertDateId: Long,

    @Column(name = "seat_number", nullable = false, length = 10)
    val seatNumber: String,

    @Column(name = "seat_grade", nullable = false, length = 20)
    val seatGrade: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false)
    var seatStatus: SeatStatus = SeatStatus.AVAILABLE
) : BaseEntity() {
    fun reserve(): Boolean {
        if (!seatStatus.isReservable()) {
            return false
        }
        seatStatus = SeatStatus.RESERVED
        return true
    }

    fun confirmSale() {
        require(seatStatus == SeatStatus.RESERVED) { "예약된 좌석만 판매 확정 가능합니다" }
        seatStatus = SeatStatus.SOLD
    }

    fun release() {
        require(seatStatus == SeatStatus.RESERVED) { "예약된 좌석만 해제 가능합니다" }
        seatStatus = SeatStatus.AVAILABLE
    }

    fun isAvailable(): Boolean = seatStatus == SeatStatus.AVAILABLE
    fun isReserved(): Boolean = seatStatus == SeatStatus.RESERVED
    fun isSold(): Boolean = seatStatus == SeatStatus.SOLD
}