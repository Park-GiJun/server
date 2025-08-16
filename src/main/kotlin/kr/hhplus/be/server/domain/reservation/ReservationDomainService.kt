package kr.hhplus.be.server.domain.reservation

import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.exception.SeatAlreadyBookedException
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.reservation.exception.InvalidReservationStatusException
import kr.hhplus.be.server.domain.reservation.exception.ReservationExpiredException
import java.time.LocalDateTime

class ReservationDomainService {

    fun validateTempReservationCreation(
        token: QueueToken,
        userId: String,
        seat: ConcertSeat,
        existingTempReservation: TempReservation?
    ) {
        if (token.userId != userId) {
            throw InvalidTokenException("Token user ID (${token.userId}) does not match request user ID ($userId)")
        }

        if (!seat.isAvailable()) {
            throw SeatAlreadyBookedException(seat.seatNumber)
        }

        if (existingTempReservation != null && existingTempReservation.isReserved()) {
            throw SeatAlreadyBookedException(seat.seatNumber)
        }
    }

    fun createTempReservation(userId: String, concertSeatId: Long): TempReservation {
        return TempReservation(
            tempReservationId = 0L,
            userId = userId,
            concertSeatId = concertSeatId,
            expiredAt = LocalDateTime.now().plusMinutes(5),
            status = TempReservationStatus.RESERVED
        )
    }

    fun validateTempReservationConfirmation(
        token: QueueToken,
        tempReservation: TempReservation
    ) {
        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(tempReservation.tempReservationId)
        }

        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(tempReservation.status, TempReservationStatus.RESERVED)
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }
    }

    fun createConfirmedReservation(
        tempReservation: TempReservation,
        seat: ConcertSeat,
        paymentAmount: Int
    ): Reservation {
        return Reservation(
            reservationId = 0L,
            userId = tempReservation.userId,
            concertDateId = seat.concertDateId,
            seatId = seat.concertSeatId,
            reservationAt = System.currentTimeMillis(),
            cancelAt = 0,
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = paymentAmount
        )
    }

    fun validateTempReservationCancellation(
        token: QueueToken,
        tempReservation: TempReservation
    ) {
        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(tempReservation.tempReservationId)
        }

        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(tempReservation.status, TempReservationStatus.RESERVED)
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }
    }
}