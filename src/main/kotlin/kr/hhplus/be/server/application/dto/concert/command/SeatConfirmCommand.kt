package kr.hhplus.be.server.application.dto.concert.command

data class SeatConfirmCommand(
    val sagaId: String,
    val seatId: Long
)