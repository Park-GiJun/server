package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand
import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.ProcessPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.queue.CompleteQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenDomainService
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.lock.DistributedLockType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PaymentCommandService(
    private val paymentRepository: PaymentRepository,
    private val tempReservationRepository: TempReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val completeTokenUseCase: CompleteQueueTokenUseCase,
    private val paymentDomainService: PaymentDomainService,
    private val userDomainService: UserDomainService,
    private val queueTokenDomainService: QueueTokenDomainService
) : ProcessPaymentUseCase {

    private val log = LoggerFactory.getLogger(PaymentCommandService::class.java)

    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "lock:payment:user:#{#command.userId}",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun processPayment(command: ProcessPaymentCommand): PaymentResult {
        log.info("결제 처리 시작: reservationId=${command.reservationId}, pointsToUse=${command.pointsToUse}")

        // 1. 결제 금액 기본 검증
        paymentDomainService.validatePaymentAmount(command.pointsToUse)

        // 2. 토큰 검증
        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )
        val token = queueTokenDomainService.createFromValidationResult(tokenResult)

        // 3. 임시 예약 조회 및 검증
        val tempReservation = tempReservationRepository.findByTempReservationId(command.reservationId)
            ?: throw TempReservationNotFoundException(command.reservationId)

        paymentDomainService.validateTempReservationForPayment(tempReservation)
        paymentDomainService.validateReservationOwnership(tempReservation, token)

        // 4. 중복 결제 방지
        val existingPayment = paymentRepository.findByReservationId(command.reservationId)
        paymentDomainService.validatePaymentNotProcessed(existingPayment, command.reservationId)

        // 5. 사용자 조회 및 검증
        val user = userRepository.findByUserId(token.userId)
        userDomainService.validateUserExists(user, token.userId)

        // 6. 좌석 정보 조회
        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        // 7. 좌석 등급 정보 조회
        val seatGrades = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, token.concertId)
        val seatGrade = seatGrades.firstOrNull()
            ?: throw ConcertNotFoundException(token.concertId)

        // 8. 결제 금액 계산
        val paymentCalculation = paymentDomainService.calculatePaymentAmounts(
            seatGrade, command.pointsToUse, user!!.availablePoint
        )

        log.info("결제 금액 계산: 총액=${paymentCalculation.totalAmount}, 포인트=${paymentCalculation.pointsToUse}, 실결제=${paymentCalculation.actualAmount}")

        // 9. 잔액 검증
        userDomainService.validateSufficientBalance(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )

        // 10. 포인트 차감
        val updatedUser = userDomainService.useUserPoint(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )
        userRepository.save(updatedUser)

        // 11. 결제 정보 생성 및 저장
        val payment = paymentDomainService.createPayment(
            command.reservationId, token.userId, paymentCalculation
        )
        val savedPayment = paymentRepository.save(payment)

        // 12. 임시 예약 확정
        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        // 13. 좌석 판매 완료 처리
        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        // 14. 예약 정보 생성 (수정된 필드명 사용)
        val reservation = Reservation(
            reservationId = 0L,
            userId = token.userId,
            concertDateId = seat.concertDateId,
            seatId = tempReservation.concertSeatId,
            reservationAt = System.currentTimeMillis(),
            cancelAt = 0,
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = paymentCalculation.actualAmount
        )
        reservationRepository.save(reservation)

        // 15. 포인트 사용 내역 저장 (포인트 사용한 경우만)
        if (paymentCalculation.pointsToUse > 0) {
            val pointHistory = PointHistory(
                pointHistoryId = 0L,
                userId = token.userId,
                pointHistoryType = "USED",
                pointHistoryAmount = paymentCalculation.pointsToUse,
                description = "Concert ticket payment"
            )
            pointHistoryRepository.save(pointHistory)
            log.info("포인트 사용 내역 저장: ${paymentCalculation.pointsToUse}원")
        }

        // 16. 토큰 완료 처리
        completeTokenUseCase.completeToken(CompleteQueueTokenCommand(command.tokenId))

        log.info("결제 처리 완료: paymentId=${savedPayment.paymentId}, userId=${token.userId}")
        return PaymentMapper.toResult(savedPayment, "Payment completed successfully")
    }
}