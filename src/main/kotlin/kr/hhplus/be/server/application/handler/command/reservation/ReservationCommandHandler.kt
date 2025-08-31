package kr.hhplus.be.server.application.handler.command.reservation

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.dto.queue.command.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ValidateQueueTokenCommand
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.application.dto.reservation.command.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.TempReservationResult
import kr.hhplus.be.server.application.port.`in`.reservation.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.reservation.ReservationDomainService
import kr.hhplus.be.server.domain.queue.service.QueueTokenDomainService
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.application.mapper.ReservationMapper
import kr.hhplus.be.server.application.port.`in`.queue.CompleteQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.domain.lock.DistributedLockType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationCommandHandler(
    private val tempReservationRepository: TempReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val completeTokenUseCase: CompleteQueueTokenUseCase,
) : CancelReservationUseCase, TempReservationUseCase, ConfirmTempReservationUseCase {

    private val log = LoggerFactory.getLogger(ReservationCommandHandler::class.java)
    private val reservationDomainService = ReservationDomainService()
    private val queueTokenDomainService = QueueTokenDomainService()

    @Transactional
    @DistributedLock(
        type = DistributedLockType.TEMP_RESERVATION_SEAT,
        key = "'lock:temp_reservation:seat:' + #command.concertSeatId",
        waitTime = 0L,
        leaseTime = 10L
    )
    override fun tempReservation(command: TempReservationCommand): TempReservationResult {
        // 1. 대기열 토큰 검증
        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )

        // 2. 사용자 존재 여부 확인
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        // 3. 좌석 조회
        val seat = concertSeatRepository.findByConcertSeatId(command.concertSeatId)
            ?: throw ConcertSeatNotFoundException(command.concertSeatId)

        // 4. 해당 좌석에 대한 기존 임시 예약 확인 (좌석별 중복 방지)
        val existingTempReservation = tempReservationRepository.findByConcertSeatId(command.concertSeatId)

        // 5. 토큰 생성 (도메인 서비스용)
        val token = queueTokenDomainService.createFromValidationResult(tokenResult, QueueTokenStatus.WAITING)

        // 6. 임시 예약 생성 검증 (좌석 상태 및 기존 예약 확인)
        reservationDomainService.validateTempReservationCreation(
            token, command.userId, seat, existingTempReservation
        )

        // 7. 임시 예약 생성
        val tempReservation = reservationDomainService.createTempReservation(
            command.userId, command.concertSeatId
        )

        // 8. 좌석 상태를 RESERVED로 변경
        val updatedSeat = seat.reserve()
        concertSeatRepository.save(updatedSeat)

        // 9. 임시 예약 저장
        val savedTempReservation = tempReservationRepository.save(tempReservation)

        log.info("임시 예약 생성 완료: userId=${command.userId}, seatId=${command.concertSeatId}, reservationId=${savedTempReservation.tempReservationId}")

        return ReservationMapper.toTempReservationResult(savedTempReservation)
    }

    @Transactional
    @DistributedLock(
        type = DistributedLockType.TEMP_RESERVATION_PROCESS,
        key = "'lock:temp_reservation:process:' + #command.tempReservationId",
        waitTime = 5L,
        leaseTime = 10L
    )
    override fun confirmTempReservation(command: ConfirmTempReservationCommand): ConfirmTempReservationResult {
        // 1. 대기열 토큰 검증
        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )

        // 2. 임시 예약 조회
        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        // 3. 토큰 생성 (도메인 서비스용)
        val token = queueTokenDomainService.createFromValidationResult(tokenResult, QueueTokenStatus.COMPLETED)

        // 4. 임시 예약 확정 검증
        reservationDomainService.validateTempReservationConfirmation(token, tempReservation)

        // 5. 좌석 조회 (비관적 락)
        val seat = concertSeatRepository.findByConcertSeatIdWithLock(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        // 6. 확정 예약 생성
        val reservation = reservationDomainService.createConfirmedReservation(
            tempReservation, seat, command.paymentAmount
        )

        // 7. 좌석 상태를 SOLD로 변경
        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        // 8. 임시 예약 상태를 CONFIRMED로 변경
        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        // 9. 확정 예약 저장
        val savedReservation = reservationRepository.save(reservation)

        // 10. 대기열 토큰 완료 처리
        completeTokenUseCase.completeToken(CompleteQueueTokenCommand(command.tokenId))

        log.info("임시 예약 확정 완료: reservationId=${savedReservation.reservationId}, userId=${savedReservation.userId}")

        return ReservationMapper.toConfirmTempReservationResult(savedReservation)
    }

    @Transactional
    @DistributedLock(
        type = DistributedLockType.TEMP_RESERVATION_PROCESS,
        key = "'lock:temp_reservation:process:' + #command.tempReservationId",
        waitTime = 5L,
        leaseTime = 10L
    )
    override fun cancelReservation(command: CancelReservationCommand): CancelReservationResult {
        // 1. 임시 예약 조회
        val tempReservation = tempReservationRepository.findByTempReservationId(command.tempReservationId)
            ?: throw TempReservationNotFoundException(command.tempReservationId)

        // 2. 좌석 조회 (비관적 락)
        val seat = concertSeatRepository.findByConcertSeatIdWithLock(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        // 3. 좌석 상태를 AVAILABLE로 복원
        val releasedSeat = seat.release()
        concertSeatRepository.save(releasedSeat)

        // 4. 임시 예약 상태를 EXPIRED로 변경
        val expiredTempReservation = tempReservation.expire()
        val savedTempReservation = tempReservationRepository.save(expiredTempReservation)

        log.info("임시 예약 취소 완료: reservationId=${command.tempReservationId}, seatId=${tempReservation.concertSeatId}")

        return ReservationMapper.toCancelReservationResult(savedTempReservation)
    }
}