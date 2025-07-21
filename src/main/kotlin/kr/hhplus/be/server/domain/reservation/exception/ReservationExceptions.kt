package kr.hhplus.be.server.domain.reservation.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException
import kr.hhplus.be.server.domain.common.exception.EntityStateException
import kr.hhplus.be.server.domain.reservation.TempReservationStatus

class TempReservationNotFoundException(reservationId: Long) :
    EntityNotFoundException("TempReservation", reservationId.toString())

class ReservationExpiredException(reservationId: Long) :
    EntityStateException("Reservation $reservationId has expired")

class InvalidReservationStatusException(
    currentStatus: TempReservationStatus,
    expectedStatus: TempReservationStatus
) : EntityStateException("Reservation status is $currentStatus, but $expectedStatus is expected")