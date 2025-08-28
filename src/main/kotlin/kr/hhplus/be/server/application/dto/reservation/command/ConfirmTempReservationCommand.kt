package kr.hhplus.be.server.application.dto.reservation.command

data class ConfirmTempReservationCommand(
    val tokenId: String,
    val tempReservationId: Long,
    val paymentAmount: Int
)