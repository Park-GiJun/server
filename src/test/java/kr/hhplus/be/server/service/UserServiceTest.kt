package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.port.`in`.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.GetUserUseCase
import kr.hhplus.be.server.application.port.`in`.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.service.user.UserCommandService
import kr.hhplus.be.server.application.service.user.UserQueryService
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import kr.hhplus.be.server.domain.users.exception.InvalidPointAmountException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserServiceTest {

    private val userRepository: UserRepository = mockk()

    private val getUserUseCase: GetUserUseCase = UserQueryService(userRepository)
    private val chargeUserPointUseCase: ChargeUserPointUseCase = UserCommandService(userRepository)
    private val useUserPointUseCase: UseUserPointUseCase = UserCommandService(userRepository)

    @Nested
    @DisplayName("사용자 조회")
    inner class GetUser {

        @Test
        @DisplayName("사용자 조회 성공")
        fun getUserSuccess() {
            val userId = "user123"
            val command = GetUserCommand(userId)
            val user = User(userId, "홍길동", 10000, 8000, 2000)

            every { userRepository.findByUserId(userId) } returns user

            val result = getUserUseCase.getUser(command)

            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.userName).isEqualTo("홍길동")
            assertThat(result.totalPoint).isEqualTo(10000)
            assertThat(result.availablePoint).isEqualTo(8000)
            assertThat(result.usedPoint).isEqualTo(2000)

            verify { userRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 실패")
        fun getUserNotFound() {
            val userId = "nonexistent"
            val command = GetUserCommand(userId)

            every { userRepository.findByUserId(userId) } returns null

            assertThrows<UserNotFoundException> {
                getUserUseCase.getUser(command)
            }

            verify { userRepository.findByUserId(userId) }
        }
    }

    @Nested
    @DisplayName("포인트 충전")
    inner class ChargeUserPoint {

        @Test
        @DisplayName("포인트 충전 성공")
        fun chargePointSuccess() {
            val userId = "user123"
            val amount = 5000
            val command = ChargeUserPointCommand(userId, amount)
            val originalUser = User(userId, "홍길동", 10000, 8000, 2000)
            val chargedUser = User(userId, "홍길동", 15000, 13000, 2000)

            every { userRepository.findByUserId(userId) } returns originalUser
            every { userRepository.save(any()) } returns chargedUser

            val result = chargeUserPointUseCase.chargeUserPoint(command)

            assertThat(result.totalPoint).isEqualTo(15000)
            assertThat(result.availablePoint).isEqualTo(13000)
            assertThat(result.usedPoint).isEqualTo(2000)

            verify { userRepository.findByUserId(userId) }
            verify { userRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 포인트 충전 실패")
        fun chargePointUserNotFound() {
            val userId = "nonexistent"
            val command = ChargeUserPointCommand(userId, 5000)

            every { userRepository.findByUserId(userId) } returns null

            assertThrows<UserNotFoundException> {
                chargeUserPointUseCase.chargeUserPoint(command)
            }

            verify { userRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("잘못된 충전 금액으로 실패")
        fun chargePointInvalidAmount() {
            val userId = "user123"
            val command = ChargeUserPointCommand(userId, -1000)
            val user = User(userId, "홍길동", 10000, 8000, 2000)

            every { userRepository.findByUserId(userId) } returns user

            assertThrows<InvalidPointAmountException> {
                chargeUserPointUseCase.chargeUserPoint(command)
            }

            verify { userRepository.findByUserId(userId) }
        }
    }

    @Nested
    @DisplayName("포인트 사용")
    inner class UseUserPoint {

        @Test
        @DisplayName("포인트 사용 성공")
        fun usePointSuccess() {
            val userId = "user123"
            val amount = 3000
            val command = UseUserPointCommand(userId, amount)
            val originalUser = User(userId, "홍길동", 10000, 8000, 2000)
            val usedUser = User(userId, "홍길동", 10000, 5000, 5000)

            every { userRepository.findByUserId(userId) } returns originalUser
            every { userRepository.save(any()) } returns usedUser

            val result = useUserPointUseCase.useUserPoint(command)

            assertThat(result.totalPoint).isEqualTo(10000)
            assertThat(result.availablePoint).isEqualTo(5000)
            assertThat(result.usedPoint).isEqualTo(5000)

            verify { userRepository.findByUserId(userId) }
            verify { userRepository.save(any()) }
        }

        @Test
        @DisplayName("잔액 부족으로 포인트 사용 실패")
        fun usePointInsufficientBalance() {
            val userId = "user123"
            val amount = 10000
            val command = UseUserPointCommand(userId, amount)
            val user = User(userId, "홍길동", 10000, 5000, 5000)

            every { userRepository.findByUserId(userId) } returns user

            assertThrows<InsufficientPointException> {
                useUserPointUseCase.useUserPoint(command)
            }

            verify { userRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 포인트 사용 실패")
        fun usePointUserNotFound() {
            val userId = "nonexistent"
            val command = UseUserPointCommand(userId, 1000)

            every { userRepository.findByUserId(userId) } returns null

            assertThrows<UserNotFoundException> {
                useUserPointUseCase.useUserPoint(command)
            }

            verify { userRepository.findByUserId(userId) }
        }

        @Test
        @DisplayName("잘못된 사용 금액으로 실패")
        fun usePointInvalidAmount() {
            val userId = "user123"
            val command = UseUserPointCommand(userId, 0)
            val user = User(userId, "홍길동", 10000, 8000, 2000)

            every { userRepository.findByUserId(userId) } returns user

            assertThrows<InvalidPointAmountException> {
                useUserPointUseCase.useUserPoint(command)
            }

            verify { userRepository.findByUserId(userId) }
        }
    }
}