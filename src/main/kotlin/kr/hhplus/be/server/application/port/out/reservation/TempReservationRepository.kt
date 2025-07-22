package kr.hhplus.be.server.application.port.out.reservation

import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.TempReservation

interface TempReservationRepository {
    fun save(tempReservation: Reservation) : Reservation
    fun findByTempReservation(tempReservationId: Long): TempReservation?
    fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long) : TempReservation?
}