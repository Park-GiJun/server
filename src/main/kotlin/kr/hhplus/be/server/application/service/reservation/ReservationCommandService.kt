package kr.hhplus.be.server.application.service.reservation

import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult
import kr.hhplus.be.server.application.port.`in`.reservation.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.reservation.ReservationDomainService
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.application.mapper.ReservationMapper
import kr.hhplus.be.server.application.port.`in`.queue.CompleteQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import org.slf4j.LoggerFactory
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
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val expireTokenUseCase: ExpireQueueTokenUseCase,
    private val completeTokenUseCase: CompleteQueueTokenUseCase,
) : CancelReservationUseCase, TempReservationUseCase, ConfirmTempReservationUseCase {

    private val log = LoggerFactory.getLogger(ReservationCommandService::class.java)
    private val reservationDomainService = ReservationDomainService()

    override fun tempReservation(command: TempReservationCommand): TempReservationResult {
        log.info("임시 예약 생성: userId=${command.userId}, seatId=${command.concertSeatId}")

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )

        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val seat = concertSeatRepository.findByConcertSeatIdWithLock(command.concertSeatId)
            ?: throw ConcertSeatNotFoundException(command.concertSeatId)

        val existingTempReservation = tempReservationRepository.findByTempReservationId(
            command.concertSeatId
        )

        val token = createQueueTokenFromResult(tokenResult)

        reservationDomainService.validateTempReservationCreation(
            token, command.userId, seat, existingTempReservation
        )

        val tempReservation = reservationDomainService.createTempReservation(
            command.userId, command.concertSeatId
        )

        val updatedSeat = seat.reserve()
        concertSeatRepository.save(updatedSeat)

        val savedTempReservation = tempReservationRepository.save(tempReservation)

        log.info("임시 예약 생성 완료: tempReservationId=${savedTempReservation.tempReservationId}")
        return ReservationMapper.toTempReservationResult(savedTempReservation)
    }

    override fun confirmTempReservation(command: ConfirmTempReservationCommand): ConfirmTempReservationResult {
        log.info("임시 예약 확정: tempReservationId=${command.tempReservationId}, amount=${command.paymentAmount}")

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )

        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        val token = createQueueTokenFromResult(tokenResult)

        reservationDomainService.validateTempReservationConfirmation(token, tempReservation)

        val seat = concertSeatRepository.findByConcertSeatIdWithLock(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val reservation = reservationDomainService.createConfirmedReservation(
            tempReservation, seat, command.paymentAmount
        )

        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        val savedReservation = reservationRepository.save(reservation)

        completeTokenUseCase.completeToken(CompleteQueueTokenCommand(command.tokenId))

        log.info("임시 예약 확정 완료: reservationId=${savedReservation.reservationId}")
        return ReservationMapper.toConfirmTempReservationResult(savedReservation)
    }

    override fun cancelReservation(command: CancelReservationCommand): CancelReservationResult {
        log.info("임시 예약 취소: tempReservationId=${command.tempReservationId}")

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )

        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        val token = createQueueTokenFromResult(tokenResult)

        reservationDomainService.validateTempReservationCancellation(token, tempReservation)

        val seat = concertSeatRepository.findByConcertSeatIdWithLock(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val releasedSeat = seat.release()
        concertSeatRepository.save(releasedSeat)

        val expiredTempReservation = tempReservation.expire()
        val savedTempReservation = tempReservationRepository.save(expiredTempReservation)

        expireTokenUseCase.expireToken(ExpireQueueTokenCommand(command.tokenId))

        log.info("임시 예약 취소 완료: seatId=${tempReservation.concertSeatId}")
        return ReservationMapper.toCancelReservationResult(savedTempReservation)
    }

    private fun createQueueTokenFromResult(tokenResult: ValidateQueueTokenResult): QueueToken {
        return QueueToken(
            queueTokenId = tokenResult.tokenId,
            userId = tokenResult.userId,
            concertId = tokenResult.concertId,
            tokenStatus = QueueTokenStatus.COMPLETED,
            createdAt = LocalDateTime.now(),
            enteredAt = LocalDateTime.now(),
        )
    }
}