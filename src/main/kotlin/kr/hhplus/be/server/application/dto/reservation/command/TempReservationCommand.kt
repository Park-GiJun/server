package kr.hhplus.be.server.application.dto.reservation.command

data class TempReservationCommand(
    val tokenId: String,
    val userId: String,
    val concertSeatId: Long
)