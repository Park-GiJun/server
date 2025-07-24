package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.queue.GenerateTokenCommand
import kr.hhplus.be.server.application.port.`in`.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.service.queue.QueueCommandService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class QueueCommandServiceTest {

    private val queueTokenRepository: QueueTokenRepository = mockk()
    private val userRepository: UserRepository = mockk()

    private val generateTokenUseCase: GenerateTokenUseCase = QueueCommandService(
        queueTokenRepository = queueTokenRepository,
        userRepository = userRepository
    )

    @Nested
    @DisplayName("대기열 테스트")
    inner class QueueCommandServiceTest {

        @Nested
        @DisplayName("토큰 생성")
        inner class GenerateTokenUseCaseTest {

            @Test
            @DisplayName("신규 토큰 생성 성공")
            fun generateNewTokenSuccessfully() {
                val command = GenerateTokenCommand(
                    userId = "user-1",
                    concertId = 1L
                )
                val user = User(
                    userId = "user-1",
                    userName = "테스트유저",
                    totalPoint = 10000,
                    availablePoint = 8000,
                    usedPoint = 2000
                )
                val expectedToken = QueueToken(
                    queueTokenId = "test-token-id",
                    userId = "user-1",
                    concertId = 1L,
                    tokenStatus = QueueTokenStatus.WAITING,
                    enteredAt = LocalDateTime.now()
                )

                every { userRepository.findByUserId("user-1") } returns user
                every { queueTokenRepository.findActiveTokenByUserAndConcert("user-1", 1L) } returns null
                every { queueTokenRepository.save(any()) } returns expectedToken

                val result = generateTokenUseCase.generateToken(command)

                assertThat(result).isEqualTo("test-token-id")
                verify { userRepository.findByUserId("user-1") }
                verify { queueTokenRepository.findActiveTokenByUserAndConcert("user-1", 1L) }
                verify { queueTokenRepository.save(any()) }
            }

            @Test
            @DisplayName("존재하지 않는 사용자 토큰 생성 실패")
            fun failToGenerateTokenForNonExistentUser() {
                val command = GenerateTokenCommand(
                    userId = "nonexistent-user",
                    concertId = 1L
                )

                every { userRepository.findByUserId("nonexistent-user") } returns null

                assertThrows<UserNotFoundException> {
                    generateTokenUseCase.generateToken(command)
                }

                verify { userRepository.findByUserId("nonexistent-user") }
            }

            @Test
            @DisplayName("기존 활성 토큰 반환")
            fun returnExistingActiveToken() {
                val command = GenerateTokenCommand(
                    userId = "user-1",
                    concertId = 1L
                )
                val user = User(
                    userId = "user-1",
                    userName = "테스트유저",
                    totalPoint = 10000,
                    availablePoint = 8000,
                    usedPoint = 2000
                )
                val existingToken = QueueToken(
                    queueTokenId = "queue-token-1",
                    userId = "user-1",
                    concertId = 1L,
                    tokenStatus = QueueTokenStatus.ACTIVE,
                    enteredAt = LocalDateTime.now()
                )

                every { userRepository.findByUserId("user-1") } returns user
                every { queueTokenRepository.findActiveTokenByUserAndConcert("user-1", 1L) } returns existingToken

                val result = generateTokenUseCase.generateToken(command)

                assertThat(result).isEqualTo("queue-token-1")
                verify { userRepository.findByUserId("user-1") }
                verify { queueTokenRepository.findActiveTokenByUserAndConcert("user-1", 1L) }
                verify(exactly = 0) { queueTokenRepository.save(any()) }
            }
        }
    }
}