package kr.hhplus.be.server.application.service.reservation

import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult
import kr.hhplus.be.server.application.port.`in`.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.TempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.concert.exception.SeatAlreadyBookedException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.InvalidReservationStatusException
import kr.hhplus.be.server.domain.reservation.exception.ReservationExpiredException
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ReservationCommandService(
    private val tempReservationRepository: TempReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val completeTokenUseCase: CompleteTokenUseCase
) : CancelReservationUseCase, TempReservationUseCase, ConfirmTempReservationUseCase {

    override fun tempReservation(command: TempReservationCommand): TempReservationResult {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        if (token.userId != command.userId) {
            throw InvalidTokenException("Token user ID (${token.userId}) does not match request user ID (${command.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(command.concertSeatId)
            ?: throw ConcertSeatNotFoundException(command.concertSeatId)

        if (!seat.isAvailable()) {
            throw SeatAlreadyBookedException("Seat is already booked")
        }

        val existingTempReservation = tempReservationRepository.findByUserIdAndConcertSeatId(
            command.userId, command.concertSeatId
        )

        if (existingTempReservation != null && existingTempReservation.isReserved()) {
            throw SeatAlreadyBookedException("Seat is already temporarily reserved by this user")
        }

        val tempReservation = TempReservation(
            tempReservationId = 0L,
            userId = command.userId,
            concertSeatId = command.concertSeatId,
            expiredAt = LocalDateTime.now().plusMinutes(5),
            status = TempReservationStatus.RESERVED
        )

        val updatedSeat = seat.reserve()
        concertSeatRepository.save(updatedSeat)

        val savedTempReservation = tempReservationRepository.save(tempReservation)

        return TempReservationResult(
            tempReservationId = savedTempReservation.tempReservationId,
            userId = savedTempReservation.userId,
            concertSeatId = savedTempReservation.concertSeatId,
            expiredAt = savedTempReservation.expiredAt,
            status = savedTempReservation.status
        )
    }

    override fun confirmTempReservation(command: ConfirmTempReservationCommand): ConfirmTempReservationResult {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(command.tempReservationId)
        }

        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(tempReservation.status, TempReservationStatus.RESERVED)
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val reservation = Reservation(
            reservationId = 0L,
            userId = tempReservation.userId,
            concertDateId = seat.concertDateId,
            seatId = seat.concertSeatId,
            reservationAt = System.currentTimeMillis(),
            cancelAt = 0,
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = command.paymentAmount
        )

        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        val savedReservation = reservationRepository.save(reservation)

        completeTokenUseCase.completeToken(CompleteTokenCommand(command.tokenId))

        return ConfirmTempReservationResult(
            reservationId = savedReservation.reservationId,
            userId = savedReservation.userId,
            concertDateId = savedReservation.concertDateId,
            seatId = savedReservation.seatId,
            reservationStatus = savedReservation.reservationStatus,
            paymentAmount = savedReservation.paymentAmount,
            reservationAt = LocalDateTime.now()
        )
    }

    override fun cancelReservation(command: CancelReservationCommand): CancelReservationResult {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        if (tempReservation.isExpired()) {
            throw ReservationExpiredException(command.tempReservationId)
        }

        if (!tempReservation.isReserved()) {
            throw InvalidReservationStatusException(tempReservation.status, TempReservationStatus.RESERVED)
        }

        if (tempReservation.userId != token.userId) {
            throw InvalidTokenException("Temporary reservation user (${tempReservation.userId}) does not match token user (${token.userId})")
        }

        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val releasedSeat = seat.release()
        concertSeatRepository.save(releasedSeat)

        val expiredTempReservation = tempReservation.expire()
        val savedTempReservation = tempReservationRepository.save(expiredTempReservation)

        expireTokenUseCase.expireToken(ExpireTokenCommand(command.tokenId))

        return CancelReservationResult(
            tempReservationId = savedTempReservation.tempReservationId,
            userId = savedTempReservation.userId,
            concertSeatId = savedTempReservation.concertSeatId,
            expiredAt = savedTempReservation.expiredAt,
            status = savedTempReservation.status
        )
    }
}