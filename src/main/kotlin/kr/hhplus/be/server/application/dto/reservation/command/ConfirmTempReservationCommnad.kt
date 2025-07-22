package kr.hhplus.be.server.application.dto.reservation.command

data class ConfirmTempReservationCommand (
    val tempReservationId: Long,
    val paymentAmount: Int
)