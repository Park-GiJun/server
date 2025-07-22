package kr.hhplus.be.server.application.dto.reservation.result

import kr.hhplus.be.server.domain.reservation.ReservationStatus
import java.time.LocalDateTime

data class ConfirmTempReservationResult(
    val reservationId: Long,
    val userId: String,
    val concertDateId: Long,
    val seatId: Long,
    val reservationStatus: ReservationStatus,
    val paymentAmount: Int,
    val reservationAt: LocalDateTime
)
