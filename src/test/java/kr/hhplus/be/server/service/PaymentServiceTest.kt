package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.ProcessPaymentCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.port.`in`.queue.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.payment.GetPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.payment.GetUserPaymentsUseCase
import kr.hhplus.be.server.application.port.`in`.payment.ProcessPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.service.payment.PaymentCommandService
import kr.hhplus.be.server.application.service.payment.PaymentQueryService
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.payment.exception.PaymentAlreadyProcessedException
import kr.hhplus.be.server.domain.payment.exception.PaymentNotFoundException
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mockk()
    private val tempReservationRepository: TempReservationRepository = mockk()
    private val reservationRepository: ReservationRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val concertSeatRepository: ConcertSeatRepository = mockk()
    private val concertSeatGradeRepository: ConcertSeatGradeRepository = mockk()
    private val pointHistoryRepository: PointHistoryRepository = mockk()
    private val validateTokenUseCase: ValidateTokenUseCase = mockk()
    private val completeTokenUseCase: CompleteTokenUseCase = mockk()

    private val processPaymentUseCase: ProcessPaymentUseCase = PaymentCommandService(
        paymentRepository, tempReservationRepository, reservationRepository,
        userRepository, concertSeatRepository, concertSeatGradeRepository,
        pointHistoryRepository, validateTokenUseCase, completeTokenUseCase
    )

    private val getPaymentUseCase: GetPaymentUseCase = PaymentQueryService(
        paymentRepository, validateTokenUseCase
    )

    private val getUserPaymentsUseCase: GetUserPaymentsUseCase = PaymentQueryService(
        paymentRepository, validateTokenUseCase
    )

    @Nested
    @DisplayName("결제 처리")
    inner class ProcessPayment {

        @Test
        @DisplayName("결제 처리 성공")
        fun processPaymentSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val reservationId = 1L
            val pointsToUse = 50000
            val command = ProcessPaymentCommand(tokenId, reservationId, pointsToUse)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val tempReservation = TempReservation(reservationId, userId, 1L, LocalDateTime.now().plusMinutes(5))
            val user = User(userId, "홍길동", 300000, 250000, 50000)
            val seat = ConcertSeat(1L, 1L, "A1", "VIP", SeatStatus.RESERVED)
            val seatGrade = ConcertSeatGrade(1L, 1L, "VIP", 170000)
            val payment = Payment(1L, reservationId, userId, 170000, pointsToUse, 120000, LocalDateTime.now(), false, false, null)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(reservationId) } returns tempReservation
            every { paymentRepository.findByReservationId(reservationId) } returns null
            every { userRepository.findByUserId(userId) } returns user
            every { concertSeatRepository.findByConcertSeatId(1L) } returns seat
            every { concertSeatGradeRepository.findBySeatGrade("VIP", 1L) } returns listOf(seatGrade)
            every { userRepository.save(any()) } returns user
            every { paymentRepository.save(any()) } returns payment
            every { tempReservationRepository.save(any()) } returns tempReservation.confirm()
            every { concertSeatRepository.save(any()) } returns seat.sell()
            every { reservationRepository.save(any()) } returns mockk()
            every { pointHistoryRepository.save(any()) } returns mockk()
            every { completeTokenUseCase.completeToken(any()) } returns true

            val result = processPaymentUseCase.processPayment(command)

            assertThat(result.paymentId).isEqualTo(1L)
            assertThat(result.reservationId).isEqualTo(reservationId)
            assertThat(result.totalAmount).isEqualTo(170000)
            assertThat(result.pointsUsed).isEqualTo(pointsToUse)
            assertThat(result.actualAmount).isEqualTo(120000)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(reservationId) }
            verify { paymentRepository.findByReservationId(reservationId) }
            verify { userRepository.findByUserId(userId) }
            verify { paymentRepository.save(any()) }
            verify { completeTokenUseCase.completeToken(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 임시 예약 결제 실패")
        fun processPaymentReservationNotFound() {
            val tokenId = "token123"
            val reservationId = 999L
            val command = ProcessPaymentCommand(tokenId, reservationId, 0)

            val tokenResult = ValidateTokenResult(tokenId, "user123", 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(reservationId) } returns null

            assertThrows<TempReservationNotFoundException> {
                processPaymentUseCase.processPayment(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(reservationId) }
        }

        @Test
        @DisplayName("이미 처리된 결제 실패")
        fun processPaymentAlreadyProcessed() {
            val tokenId = "token123"
            val userId = "user123"
            val reservationId = 1L
            val command = ProcessPaymentCommand(tokenId, reservationId, 0)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val tempReservation = TempReservation(reservationId, userId, 1L, LocalDateTime.now().plusMinutes(5))
            val existingPayment = Payment(1L, reservationId, userId, 170000, 0, 170000, LocalDateTime.now(), false, false, null)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(reservationId) } returns tempReservation
            every { paymentRepository.findByReservationId(reservationId) } returns existingPayment

            assertThrows<PaymentAlreadyProcessedException> {
                processPaymentUseCase.processPayment(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(reservationId) }
            verify { paymentRepository.findByReservationId(reservationId) }
        }

        @Test
        @DisplayName("잔액 부족으로 결제 실패")
        fun processPaymentInsufficientBalance() {
            val tokenId = "token123"
            val userId = "user123"
            val reservationId = 1L
            val command = ProcessPaymentCommand(tokenId, reservationId, 0)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val tempReservation = TempReservation(reservationId, userId, 1L, LocalDateTime.now().plusMinutes(5))
            val user = User(userId, "홍길동", 50000, 30000, 20000)
            val seat = ConcertSeat(1L, 1L, "A1", "VIP", SeatStatus.RESERVED)
            val seatGrade = ConcertSeatGrade(1L, 1L, "VIP", 170000)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(reservationId) } returns tempReservation
            every { paymentRepository.findByReservationId(reservationId) } returns null
            every { userRepository.findByUserId(userId) } returns user
            every { concertSeatRepository.findByConcertSeatId(1L) } returns seat
            every { concertSeatGradeRepository.findBySeatGrade("VIP", 1L) } returns listOf(seatGrade)

            assertThrows<InsufficientPointException> {
                processPaymentUseCase.processPayment(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(reservationId) }
            verify { paymentRepository.findByReservationId(reservationId) }
            verify { userRepository.findByUserId(userId) }
        }
    }

    @Nested
    @DisplayName("결제 내역 조회")
    inner class GetPayment {

        @Test
        @DisplayName("결제 내역 조회 성공")
        fun getPaymentSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val paymentId = 1L
            val command = GetPaymentCommand(tokenId, paymentId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val payment = Payment(paymentId, 1L, userId, 170000, 50000, 120000, LocalDateTime.now(), false, false, null)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { paymentRepository.findByPaymentId(paymentId) } returns payment

            val result = getPaymentUseCase.getPayment(command)

            assertThat(result.paymentId).isEqualTo(paymentId)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.totalAmount).isEqualTo(170000)
            assertThat(result.pointsUsed).isEqualTo(50000)
            assertThat(result.actualAmount).isEqualTo(120000)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { paymentRepository.findByPaymentId(paymentId) }
        }

        @Test
        @DisplayName("존재하지 않는 결제 내역 조회 실패")
        fun getPaymentNotFound() {
            val tokenId = "token123"
            val paymentId = 999L
            val command = GetPaymentCommand(tokenId, paymentId)

            val tokenResult = ValidateTokenResult(tokenId, "user123", 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { paymentRepository.findByPaymentId(paymentId) } returns null

            assertThrows<PaymentNotFoundException> {
                getPaymentUseCase.getPayment(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { paymentRepository.findByPaymentId(paymentId) }
        }
    }

    @Nested
    @DisplayName("사용자 결제 내역 목록 조회")
    inner class GetUserPayments {

        @Test
        @DisplayName("사용자 결제 내역 목록 조회 성공")
        fun getUserPaymentsSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val command = GetUserPaymentsCommand(tokenId, userId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val payments = listOf(
                Payment(1L, 1L, userId, 170000, 50000, 120000, LocalDateTime.now(), false, false, null),
                Payment(2L, 2L, userId, 120000, 20000, 100000, LocalDateTime.now().minusDays(1), false, false, null)
            )

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { paymentRepository.findByUserId(userId) } returns payments

            val result = getUserPaymentsUseCase.getUserPayments(command)

            assertThat(result).hasSize(2)
            assertThat(result[0].paymentId).isEqualTo(1L)
            assertThat(result[1].paymentId).isEqualTo(2L)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { paymentRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("결제 내역이 없는 경우 빈 목록 반환")
        fun getUserPaymentsEmpty() {
            val tokenId = "token123"
            val userId = "user123"
            val command = GetUserPaymentsCommand(tokenId, userId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { paymentRepository.findByUserId(userId) } returns emptyList()

            val result = getUserPaymentsUseCase.getUserPayments(command)

            assertThat(result).isEmpty()

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { paymentRepository.findByUserId(userId) }
        }
    }
}