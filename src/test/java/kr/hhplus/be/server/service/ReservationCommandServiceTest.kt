package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.TempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.service.reservation.ReservationCommandService
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.concert.exception.SeatAlreadyBookedException
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.TempReservationNotFoundException
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class ReservationCommandServiceTest {

    private val tempReservationRepository: TempReservationRepository = mockk()
    private val reservationRepository: ReservationRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val concertSeatRepository: ConcertSeatRepository = mockk()
    private val validateTokenUseCase: ValidateTokenUseCase = mockk()
    private val expireTokenUseCase: ExpireTokenUseCase = mockk()
    private val completeTokenUseCase: CompleteTokenUseCase = mockk()

    private val tempReservationUseCase: TempReservationUseCase = ReservationCommandService(
        tempReservationRepository, reservationRepository, userRepository,
        concertSeatRepository, validateTokenUseCase, expireTokenUseCase, completeTokenUseCase
    )
    private val confirmTempReservationUseCase: ConfirmTempReservationUseCase = ReservationCommandService(
        tempReservationRepository, reservationRepository, userRepository,
        concertSeatRepository, validateTokenUseCase, expireTokenUseCase, completeTokenUseCase
    )
    private val cancelReservationUseCase: CancelReservationUseCase = ReservationCommandService(
        tempReservationRepository, reservationRepository, userRepository,
        concertSeatRepository, validateTokenUseCase, expireTokenUseCase, completeTokenUseCase
    )

    @Nested
    @DisplayName("임시 예약 생성")
    inner class TempReservation {

        @Test
        @DisplayName("임시 예약 생성 성공")
        fun tempReservationSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val concertSeatId = 1L
            val command = TempReservationCommand(tokenId, userId, concertSeatId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val user = User(userId, "홍길동", 10000, 8000, 2000)
            val seat = ConcertSeat(concertSeatId, 1L, "A1", "VIP", SeatStatus.AVAILABLE)
            val savedTempReservation = TempReservation(1L, userId, concertSeatId, LocalDateTime.now().plusMinutes(5))

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { userRepository.findByUserId(userId) } returns user
            every { concertSeatRepository.findByConcertSeatId(concertSeatId) } returns seat
            every { tempReservationRepository.findByUserIdAndConcertSeatId(userId, concertSeatId) } returns null
            every { concertSeatRepository.save(any()) } returns seat.reserve()
            every { tempReservationRepository.save(any()) } returns savedTempReservation

            val result = tempReservationUseCase.tempReservation(command)

            assertThat(result.tempReservationId).isEqualTo(1L)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.concertSeatId).isEqualTo(concertSeatId)
            assertThat(result.status).isEqualTo(TempReservationStatus.RESERVED)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { userRepository.findByUserId(userId) }
            verify { concertSeatRepository.findByConcertSeatId(concertSeatId) }
            verify { tempReservationRepository.findByUserIdAndConcertSeatId(userId, concertSeatId) }
            verify { concertSeatRepository.save(any()) }
            verify { tempReservationRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 임시 예약 실패")
        fun tempReservationUserNotFound() {
            val tokenId = "token123"
            val userId = "nonexistent"
            val concertSeatId = 1L
            val command = TempReservationCommand(tokenId, userId, concertSeatId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { userRepository.findByUserId(userId) } returns null

            assertThrows<UserNotFoundException> {
                tempReservationUseCase.tempReservation(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { userRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("존재하지 않는 좌석으로 임시 예약 실패")
        fun tempReservationSeatNotFound() {
            val tokenId = "token123"
            val userId = "user123"
            val concertSeatId = 999L
            val command = TempReservationCommand(tokenId, userId, concertSeatId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val user = User(userId, "홍길동", 10000, 8000, 2000)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { userRepository.findByUserId(userId) } returns user
            every { concertSeatRepository.findByConcertSeatId(concertSeatId) } returns null

            assertThrows<ConcertSeatNotFoundException> {
                tempReservationUseCase.tempReservation(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { userRepository.findByUserId(userId) }
            verify { concertSeatRepository.findByConcertSeatId(concertSeatId) }
        }

        @Test
        @DisplayName("이미 예약된 좌석으로 임시 예약 실패")
        fun tempReservationSeatAlreadyBooked() {
            val tokenId = "token123"
            val userId = "user123"
            val concertSeatId = 1L
            val command = TempReservationCommand(tokenId, userId, concertSeatId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val user = User(userId, "홍길동", 10000, 8000, 2000)
            val seat = ConcertSeat(concertSeatId, 1L, "A1", "VIP", SeatStatus.RESERVED)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { userRepository.findByUserId(userId) } returns user
            every { concertSeatRepository.findByConcertSeatId(concertSeatId) } returns seat
            every { tempReservationRepository.findByUserIdAndConcertSeatId(userId, concertSeatId) } returns null

            assertThrows<SeatAlreadyBookedException> {
                tempReservationUseCase.tempReservation(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { userRepository.findByUserId(userId) }
            verify { concertSeatRepository.findByConcertSeatId(concertSeatId) }
            verify { tempReservationRepository.findByUserIdAndConcertSeatId(userId, concertSeatId) }
        }
    }

    @Nested
    @DisplayName("임시 예약 확정")
    inner class ConfirmTempReservation {

        @Test
        @DisplayName("임시 예약 확정 성공")
        fun confirmTempReservationSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val tempReservationId = 1L
            val paymentAmount = 170000
            val command = ConfirmTempReservationCommand(tokenId, tempReservationId, paymentAmount)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val tempReservation = TempReservation(tempReservationId, userId, 1L, LocalDateTime.now().plusMinutes(5))
            val seat = ConcertSeat(1L, 1L, "A1", "VIP", SeatStatus.RESERVED)
            val reservation = mockk<kr.hhplus.be.server.domain.reservation.Reservation>()

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(tempReservationId) } returns tempReservation
            every { concertSeatRepository.findByConcertSeatId(1L) } returns seat
            every { concertSeatRepository.save(any()) } returns seat.sell()
            every { tempReservationRepository.save(any()) } returns tempReservation.confirm()
            every { reservationRepository.save(any()) } returns reservation
            every { completeTokenUseCase.completeToken(any()) } returns true

            every { reservation.reservationId } returns 1L
            every { reservation.userId } returns userId
            every { reservation.concertDateId } returns 1L
            every { reservation.seatId } returns 1L
            every { reservation.reservationStatus } returns kr.hhplus.be.server.domain.reservation.ReservationStatus.CONFIRMED
            every { reservation.paymentAmount } returns paymentAmount

            val result = confirmTempReservationUseCase.confirmTempReservation(command)

            assertThat(result.reservationId).isEqualTo(1L)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.paymentAmount).isEqualTo(paymentAmount)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(tempReservationId) }
            verify { concertSeatRepository.findByConcertSeatId(1L) }
            verify { concertSeatRepository.save(any()) }
            verify { tempReservationRepository.save(any()) }
            verify { reservationRepository.save(any()) }
            verify { completeTokenUseCase.completeToken(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 임시 예약 확정 실패")
        fun confirmTempReservationNotFound() {
            val tokenId = "token123"
            val tempReservationId = 999L
            val command = ConfirmTempReservationCommand(tokenId, tempReservationId, 170000)

            val tokenResult = ValidateTokenResult(tokenId, "user123", 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(tempReservationId) } returns null

            assertThrows<TempReservationNotFoundException> {
                confirmTempReservationUseCase.confirmTempReservation(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(tempReservationId) }
        }
    }

    @Nested
    @DisplayName("임시 예약 취소")
    inner class CancelReservation {

        @Test
        @DisplayName("임시 예약 취소 성공")
        fun cancelReservationSuccess() {
            val tokenId = "token123"
            val userId = "user123"
            val tempReservationId = 1L
            val command = CancelReservationCommand(tokenId, tempReservationId)

            val tokenResult = ValidateTokenResult(tokenId, userId, 1L, true)
            val tempReservation = TempReservation(tempReservationId, userId, 1L, LocalDateTime.now().plusMinutes(5))
            val seat = ConcertSeat(1L, 1L, "A1", "VIP", SeatStatus.RESERVED)
            val expiredTempReservation = tempReservation.expire()

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(tempReservationId) } returns tempReservation
            every { concertSeatRepository.findByConcertSeatId(1L) } returns seat
            every { concertSeatRepository.save(any()) } returns seat.release()
            every { tempReservationRepository.save(any()) } returns expiredTempReservation
            every { expireTokenUseCase.expireToken(any()) } returns true

            val result = cancelReservationUseCase.cancelReservation(command)

            assertThat(result.tempReservationId).isEqualTo(tempReservationId)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.status).isEqualTo(TempReservationStatus.EXPIRED)

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(tempReservationId) }
            verify { concertSeatRepository.findByConcertSeatId(1L) }
            verify { concertSeatRepository.save(any()) }
            verify { tempReservationRepository.save(any()) }
            verify { expireTokenUseCase.expireToken(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 임시 예약 취소 실패")
        fun cancelReservationNotFound() {
            val tokenId = "token123"
            val tempReservationId = 999L
            val command = CancelReservationCommand(tokenId, tempReservationId)

            val tokenResult = ValidateTokenResult(tokenId, "user123", 1L, true)

            every { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) } returns tokenResult
            every { tempReservationRepository.findByTempReservationId(tempReservationId) } returns null

            assertThrows<TempReservationNotFoundException> {
                cancelReservationUseCase.cancelReservation(command)
            }

            verify { validateTokenUseCase.validateActiveToken(ValidateTokenCommand(tokenId)) }
            verify { tempReservationRepository.findByTempReservationId(tempReservationId) }
        }
    }
}