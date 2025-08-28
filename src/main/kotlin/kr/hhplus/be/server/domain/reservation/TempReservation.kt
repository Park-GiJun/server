package kr.hhplus.be.server.domain.reservation

import java.time.LocalDateTime

class TempReservation(
    var tempReservationId: Long = 0,
    val userId: String,
    val concertSeatId: Long,
    val expiredAt: LocalDateTime,
    val status: TempReservationStatus = TempReservationStatus.RESERVED,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
) {

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiredAt)
    fun isReserved(): Boolean = status == TempReservationStatus.RESERVED
    fun isConfirmed(): Boolean = status == TempReservationStatus.CONFIRMED

    fun confirm(): TempReservation {
        return TempReservation(
            tempReservationId = this.tempReservationId,
            userId = this.userId,
            concertSeatId = this.concertSeatId,
            expiredAt = this.expiredAt,
            status = TempReservationStatus.CONFIRMED
        )
    }

    fun expire(): TempReservation {
        return TempReservation(
            tempReservationId = this.tempReservationId,
            userId = this.userId,
            concertSeatId = this.concertSeatId,
            expiredAt = this.expiredAt,
            status = TempReservationStatus.EXPIRED
        )
    }

    fun delete(): TempReservation {
        return TempReservation(
            tempReservationId = this.tempReservationId,
            userId = this.userId,
            concertSeatId = this.concertSeatId,
            expiredAt = this.expiredAt,
            status = this.status,
            isDeleted = true,
            deletedAt = LocalDateTime.now()
        )
    }
}