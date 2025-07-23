package kr.hhplus.be.server.application.dto.reservation

data class TempReservationCommand(
    val tokenId: String,
    val userId: String,
    val concertSeatId: Long
)

data class ConfirmTempReservationCommand(
    val tokenId: String,
    val tempReservationId: Long,
    val paymentAmount: Int
)

data class CancelReservationCommand(
    val tokenId: String,
    val tempReservationId: Long
)
