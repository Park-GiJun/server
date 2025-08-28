package kr.hhplus.be.server.application.port.out.reservation

import kr.hhplus.be.server.domain.reservation.TempReservation

interface TempReservationRepository {
    fun save(tempReservation: TempReservation): TempReservation
    fun delete(reservation: TempReservation)
    fun findByTempReservationId(tempReservationId: Long): TempReservation?
    fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservation?
    fun findByConcertSeatId(concertSeatId: Long): TempReservation?
    fun findAll(): List<TempReservation>
}