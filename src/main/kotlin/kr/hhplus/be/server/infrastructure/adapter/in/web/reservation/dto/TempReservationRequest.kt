package kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto

import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import java.time.LocalDateTime

data class TempReservationRequest(
    val userId: String,
    val concertSeatId: Long
)

data class TempReservationResponse(
    val tempReservationId: Long,
    val userId: String,
    val concertSeatId: Long,
    val expiredAt: LocalDateTime,
    val status: TempReservationStatus
)

data class ReservationConfirmRequest(
    val tempReservationId: Long,
    val paymentAmount: Int
)

data class ReservationResponse(
    val reservationId: Long,
    val userId: String,
    val concertDateId: Long,
    val seatId: Long,
    val reservationStatus: ReservationStatus,
    val paymentAmount: Int,
    val reservationAt: LocalDateTime
)

data class ReservationCancelRequest(
    val tempReservationId: Long
)