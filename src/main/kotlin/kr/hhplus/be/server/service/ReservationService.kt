package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
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

    fun createTempReservation(tokenId: String, request: TempReservationRequest): TempReservation {
        val token = queueService.validateActiveToken(tokenId)

        validateUser(request.userId)

        if (token.userId != request.userId) {
            throw InvalidTokenException("Token user ID (${token.userId}) does not match request user ID (${request.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(request.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found: ${request.concertSeatId}")

        if (!seat.isAvailable()) {
            throw SeatAlreadyBookedException("Seat is already booked")
        }

        val existingTempReservation = tempReservationRepository.findByUserIdAndConcertSeatId(
            request.userId, request.concertSeatId
        )

        if (existingTempReservation != null && existingTempReservation.isReserved()) {
            throw SeatAlreadyBookedException("Seat is already temporarily reserved by this user")
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

    fun confirmReservation(tokenId: String, request: ReservationConfirmRequest): Reservation {
        val token = queueService.validateActiveToken(tokenId)

        val tempReservation = tempReservationRepository.findByTempReservationId(request.tempReservationId)
            ?: throw QueueTokenNotFoundException("Temporary reservation not found: ${request.tempReservationId}")

        if (tempReservation.isExpired()) {
            throw ConcertDateExpiredException("Temporary reservation has expired")
        }

        if (!tempReservation.isReserved()) {
            throw InvalidTokenStatusException("Invalid temporary reservation status: ${tempReservation.status}")
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found: ${tempReservation.concertSeatId}")

        if (request.paymentAmount <= 0) {
            throw InvalidateAmountException("Invalid payment amount: ${request.paymentAmount}")
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

        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        val savedReservation = reservationRepository.save(reservation)

        queueService.completeToken(tokenId)

        return savedReservation
    }

    fun cancelReservation(tokenId: String, request: ReservationCancelRequest): TempReservation {
        val token = queueService.validateActiveToken(tokenId)

        val tempReservation = tempReservationRepository.findByTempReservationId(request.tempReservationId)
            ?: throw QueueTokenNotFoundException("Temporary reservation not found: ${request.tempReservationId}")

        if (tempReservation.isExpired()) {
            throw ConcertDateExpiredException("Temporary reservation has already expired")
        }

        if (!tempReservation.isReserved()) {
            throw InvalidTokenStatusException("Invalid temporary reservation status: ${tempReservation.status}")
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertNotFoundException("Concert seat not found: ${tempReservation.concertSeatId}")

        val releasedSeat = seat.release()
        concertSeatRepository.save(releasedSeat)

        val expiredTempReservation = tempReservation.expire()
        val savedTempReservation = tempReservationRepository.save(expiredTempReservation)

        queueService.expireToken(tokenId)

        return savedTempReservation
    }

    private fun validateUser(userId: String) {
        userRepository.findByUserId(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }
}