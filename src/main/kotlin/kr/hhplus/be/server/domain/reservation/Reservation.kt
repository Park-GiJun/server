package kr.hhplus.be.server.domain.reservation

import java.time.LocalDateTime

class Reservation(
    val reservationId: Long,
    val userId: String,
    val concertDateId: Long,
    val seatId: Long,
    val reservationAt: LocalDateTime,
    val cancelAt: LocalDateTime? = null,
    val reservationStatus: ReservationStatus,
    val paymentAmount: Int,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)  {
    fun cancel(): Reservation {
        return Reservation(
            reservationId = this.reservationId,
            userId = this.userId,
            concertDateId = this.concertDateId,
            reservationAt = this.reservationAt,
            reservationStatus = ReservationStatus.CANCELLED,
            cancelAt = LocalDateTime.now(),
            paymentAmount = this.paymentAmount,
            seatId = this.seatId
        )
    }
}