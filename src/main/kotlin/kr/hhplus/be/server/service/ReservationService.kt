package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.dto.QueueTokenStatusRequest
import kr.hhplus.be.server.dto.ReservationCancelRequest
import kr.hhplus.be.server.dto.ReservationConfirmRequest
import kr.hhplus.be.server.dto.TempReservationRequest
import kr.hhplus.be.server.exception.*
import kr.hhplus.be.server.repository.mock.MockConcertSeatRepository
import kr.hhplus.be.server.repository.mock.MockReservationRepository
import kr.hhplus.be.server.repository.mock.MockTempReservationRepository
import kr.hhplus.be.server.repository.mock.MockUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ReservationService(
    private val tempReservationRepository: MockTempReservationRepository,
    private val reservationRepository: MockReservationRepository,
    private val concertSeatRepository: MockConcertSeatRepository,
    private val userRepository: MockUserRepository,
    private val queueService: QueueService
) {

    fun createTempReservation(
        tokenRequest: QueueTokenStatusRequest, request: TempReservationRequest
    ): TempReservation {
        validateUser(request.userId)

        validateTokenStatus(tokenRequest)

        val seat = concertSeatRepository.findByConcertSeatId(request.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found")

        if (!seat.isAvailable()) {
            throw SeatAlreadyBookedException("Seat is already booked")
        }

        val existingTempReservation = tempReservationRepository.findByUserIdAndConcertSeatId(
            request.userId, request.concertSeatId
        )

        if (existingTempReservation != null && existingTempReservation.isReserved()) {
            throw SeatAlreadyBookedException("Seat is already temporarily reserved")
        }

        val tempReservation = TempReservation(
            userId = request.userId,
            concertSeatId = request.concertSeatId,
            expiredAt = LocalDateTime.now().plusMinutes(5),
            status = TempReservationStatus.RESERVED
        )

        val updatedSeat = seat.reserve()
        concertSeatRepository.save(updatedSeat)

        return tempReservationRepository.save(tempReservation)
    }

    fun confirmReservation(
        tokenRequest: QueueTokenStatusRequest, request: ReservationConfirmRequest
    ): Reservation {
        val tempReservation = tempReservationRepository.findByTempReservationId(request.tempReservationId)
            ?: throw QueueTokenNotFoundException("Temporary reservation not found")

        if (tempReservation.isExpired()) {
            throw ConcertDateExpiredException("Temporary reservation has expired")
        }

        if (!tempReservation.isReserved()) {
            throw InvalidTokenStatusException("Invalid temporary reservation status")
        }

        if (tempReservation.userId != tokenRequest.userId) {
            throw InvalidTokenException("User mismatch")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found")

        if (request.paymentAmount <= 0) {
            throw InvalidateAmountException("Invalid payment amount")
        }

        val reservation = Reservation(
            reservationId = 0,
            userId = tempReservation.userId,
            concertDateId = seat.concertDateId,
            seatId = seat.concertSeatId,
            reservationAt = System.currentTimeMillis(),
            cancelAt = 0,
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = request.paymentAmount
        )

        val bookedSeat = seat.sell()
        concertSeatRepository.save(bookedSeat)

        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        queueService.expireToken(tokenRequest.userId)

        return reservationRepository.save(reservation)
    }


    fun cancelReservation(
        tokenRequest: QueueTokenStatusRequest,
        request: ReservationCancelRequest
    ): TempReservation {
        val tempReservation = tempReservationRepository.findByTempReservationId(request.tempReservationId)
            ?: throw QueueTokenNotFoundException("Temporary reservation not found")

        if (tempReservation.isExpired()) {
            throw ConcertDateExpiredException("Temporary reservation has expired")
        }

        if (!tempReservation.isReserved()) {
            throw InvalidTokenStatusException("Invalid temporary reservation status")
        }

        if (tempReservation.userId != tokenRequest.userId) {
            throw InvalidTokenException("User mismatch")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found")

        val releasedSeat = seat.release()
        concertSeatRepository.save(releasedSeat)

        val expiredTempReservation = tempReservation.expire()
        tempReservationRepository.save(expiredTempReservation)

        queueService.expireToken(tokenRequest.userId)

        return expiredTempReservation
    }

    private fun validateUser(userId: String) {
        userRepository.findByUserId(userId) ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun validateTokenStatus(tokenRequest: QueueTokenStatusRequest) {
        if (tokenRequest.status != QueueTokenStatus.ACTIVE) {
            throw InvalidTokenStatusException("Token is not active")
        }
    }
}