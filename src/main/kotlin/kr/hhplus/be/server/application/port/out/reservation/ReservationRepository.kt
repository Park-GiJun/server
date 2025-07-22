package kr.hhplus.be.server.application.port.out.reservation

import kr.hhplus.be.server.domain.reservation.Reservation

interface ReservationRepository {
    fun save(reservation: Reservation) : Reservation
}