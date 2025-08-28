package kr.hhplus.be.server.application.dto.concert.command

data class SeatReleaseCommand(
    val sagaId: String,
    val seatId: Long
)