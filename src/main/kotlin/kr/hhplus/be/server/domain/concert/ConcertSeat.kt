package kr.hhplus.be.server.domain.concert

import java.time.LocalDateTime

class ConcertSeat(
    val concertSeatId: Long = 0L,
    val concertDateId: Long,
    val seatNumber: String,
    val seatGrade: String,
    val seatStatus: SeatStatus = SeatStatus.AVAILABLE,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
) {

    fun isAvailable(): Boolean = seatStatus == SeatStatus.AVAILABLE
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