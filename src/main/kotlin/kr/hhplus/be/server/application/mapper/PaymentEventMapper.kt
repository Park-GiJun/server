package kr.hhplus.be.server.application.mapper

import kr.hhplus.be.server.application.dto.event.ReservationEventDto
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent

object PaymentEventMapper {
    fun toReservationEvent(event: PaymentCompletedEvent): ReservationEventDto {
        return ReservationEventDto(
            eventType = "RESERVATION_COMPLETED",
            reservationId = event.reservationId,
            userId = event.userId,
            concertId = event.concertId,
            seatNumber = event.seatNumber,
            price = event.totalAmount,
        )
    }
}