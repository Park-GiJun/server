package kr.hhplus.be.server.application.dto.reservation.result

import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import java.time.LocalDateTime

data class CancelReservationResult(
    val tempReservationId: Long,
    val userId: String,
    val concertSeatId: Long,
    val expiredAt: LocalDateTime,
    val status: TempReservationStatus
)

data class ConfirmTempReservationResult(
    val reservationId: Long,
    val userId: String,
    val concertDateId: Long,
    val seatId: Long,
    val reservationStatus: ReservationStatus,
    val paymentAmount: Int,
    val reservationAt: LocalDateTime
)

data class TempReservationResult(
    val tempReservationId: Long,
    val userId: String,
    val concertSeatId: Long,
    val expiredAt: LocalDateTime,
    val status: TempReservationStatus
)
