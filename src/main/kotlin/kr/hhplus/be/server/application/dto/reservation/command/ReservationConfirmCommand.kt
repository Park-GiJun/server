package kr.hhplus.be.server.application.dto.reservation.command

data class ReservationConfirmCommand(
    val sagaId: String,
    val reservationId: Long,
    val userId: String,
    val seatId: Long,
    val concertDateId: Long,
    val paymentAmount: Int
)
