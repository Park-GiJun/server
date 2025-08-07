package kr.hhplus.be.server.domain.reservation

import java.time.LocalDateTime

class Reservation(
    val reservationId: Long,
    val userId: String,
    val concertDateId: Long,
    val seatId: Long,
    val reservationAt: Long,
    val cancelAt: Long,
    val reservationStatus: ReservationStatus,
    val paymentAmount: Int,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)