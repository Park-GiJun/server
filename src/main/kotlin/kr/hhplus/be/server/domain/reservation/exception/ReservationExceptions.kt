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

class ReservationNotReservedException(message: String) :
    EntityStateException(message)

class TempReservationValidationException(userId: String, seatId: Long) : EntityStateException(
    "Temp reservation $userId validation failed. Seat cannot be empty"
)