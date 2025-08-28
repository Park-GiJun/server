package kr.hhplus.be.server.application.handler.command.payment

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.dto.payment.result.PaymentResult
import kr.hhplus.be.server.application.dto.payment.command.ProcessPaymentCommand
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.ProcessPaymentUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.event.payment.PaymentEventPort
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.lock.DistributedLockType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentCommandHandler(
    private val paymentRepository: PaymentRepository,
    private val tempReservationRepository: TempReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val concertDateRepository: ConcertDateRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : ProcessPaymentUseCase {


    private val paymentDomainService = PaymentDomainService()
    private val userDomainService = UserDomainService()
    private val log = LoggerFactory.getLogger(PaymentCommandHandler::class.java)

    /**
     * 결제 처리
     * 실행 순서: 트랜잭션 시작 -> 분산락 획득 -> 비즈니스 로직 -> 이벤트 발행 -> 락 해제 -> 트랜잭션 종료 -> 이벤트 처리
     *
     *
     * 더이상 사용하지않음.
     */
    @Transactional
    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "'lock:payment:reservation:' + #command.reservationId",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun processPayment(command: ProcessPaymentCommand): PaymentResult {
        log.info("결제 처리 시작: reservationId=${command.reservationId}, pointsToUse=${command.pointsToUse}")

        // 1. 기본 검증
        paymentDomainService.validatePaymentAmount(command.pointsToUse)

        // 2. 임시 예약 조회 및 검증
        val tempReservation = tempReservationRepository.findByTempReservationId(command.reservationId)
            ?: throw TempReservationNotFoundException(command.reservationId)
        paymentDomainService.validateTempReservationForPayment(tempReservation)

        // 3. 중복 결제 확인
        val existingPayment = paymentRepository.findByReservationId(command.reservationId)
        paymentDomainService.validatePaymentNotProcessed(existingPayment, command.reservationId)

        // 4. 사용자 조회 및 검증
        val user = userRepository.findByUserIdWithLock(tempReservation.userId)
        userDomainService.validateUserExists(user, tempReservation.userId)

        // 5. 좌석 및 가격 정보 조회
        val seat = concertSeatRepository.findByConcertSeatId(tempReservation.concertSeatId)
            ?: throw ConcertSeatNotFoundException(tempReservation.concertSeatId)

        val concert = concertDateRepository.findByConcertDateId(seat.concertDateId)
            ?: throw ConcertNotFoundException(seat.concertDateId)

        val seatGrades = concertSeatGradeRepository.findBySeatGrade(seat.seatGrade, concert.concertId)
        val seatGrade = seatGrades.firstOrNull()
            ?: throw ConcertNotFoundException(concert.concertId)

        // 6. 결제 금액 계산
        val paymentCalculation = paymentDomainService.calculatePaymentAmounts(
            seatGrade, command.pointsToUse, user!!.availablePoint
        )

        // 7. 잔액 확인
        userDomainService.validateSufficientBalance(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )

        // 8. 사용자 포인트 차감
        val updatedUser = userDomainService.useUserPoint(
            user,
            paymentCalculation.actualAmount + paymentCalculation.pointsToUse
        )
        userRepository.save(updatedUser)

        // 9. 결제 정보 저장
        val payment = paymentDomainService.createPayment(
            command.reservationId, tempReservation.userId, paymentCalculation
        )
        val savedPayment = paymentRepository.save(payment)

        // 10. 임시 예약 확정
        val confirmedTempReservation = tempReservation.confirm()
        tempReservationRepository.save(confirmedTempReservation)

        // 11. 좌석 판매 완료
        val soldSeat = seat.sell()
        concertSeatRepository.save(soldSeat)

        val reservation = Reservation(
            reservationId = 0L,
            userId = tempReservation.userId,
            concertDateId = seat.concertDateId,
            seatId = tempReservation.concertSeatId,
            reservationAt = LocalDateTime.now(),
            reservationStatus = ReservationStatus.CONFIRMED,
            paymentAmount = paymentCalculation.actualAmount
        )
        val savedReservation = reservationRepository.save(reservation)

        // 13. 포인트 사용 히스토리 저장
        if (paymentCalculation.pointsToUse > 0) {
            val pointHistory = PointHistory(
                pointHistoryId = 0L,
                userId = tempReservation.userId,
                pointHistoryType = "USED",
                pointHistoryAmount = paymentCalculation.pointsToUse,
                description = "Concert ticket payment"
            )
            pointHistoryRepository.save(pointHistory)
        }

        // 14. 결제 완료 이벤트 발행
        val paymentCompletedEvent = PaymentCompletedEvent(
            paymentId = savedPayment.paymentId,
            reservationId = savedReservation.reservationId,
            userId = tempReservation.userId,
            concertId = concert.concertId,
            seatNumber = seat.seatNumber,
            totalAmount = paymentCalculation.totalAmount
        )

        applicationEventPublisher.publishEvent(paymentCompletedEvent)

        return PaymentMapper.toResult(savedPayment, "Payment completed successfully")
    }
}