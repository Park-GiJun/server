package kr.hhplus.be.server.application.dto.event

import java.time.LocalDateTime

data class ReservationEventDto(
    val eventType: String,
    val reservationId: Long,
    val userId: String,
    val concertId: Long,
    val seatNumber: String,
    val price: Int,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)