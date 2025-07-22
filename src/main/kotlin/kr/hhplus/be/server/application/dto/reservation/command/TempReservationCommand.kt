package kr.hhplus.be.server.application.dto.reservation.command

data class TempReservationCommand(
    val userId: String,
    val concertSeatId: Long
)