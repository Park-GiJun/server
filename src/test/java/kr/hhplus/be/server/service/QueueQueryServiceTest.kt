package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.port.`in`.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.service.queue.QueueQueryService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class QueueQueryServiceTest {

    private val queueTokenRepository: QueueTokenRepository = mockk()

    private val getQueueStatusUseCase: GetQueueStatusUseCase = QueueQueryService(
        queueTokenRepository = queueTokenRepository
    )

    @Nested
    @DisplayName("대기열 조회")
    inner class GetQueueStatus {

        @Test
        @DisplayName("대기중인 토큰 상태 조회 성공")
        fun getWaitingTokenStatus() {
            val tokenId = "test-token-id"
            val query = GetQueueStatusQuery(tokenId = tokenId)
            val token = QueueToken(
                queueTokenId = tokenId,
                userId = "user-1",
                concertId = 1L,
                tokenStatus = QueueTokenStatus.WAITING,
                enteredAt = LocalDateTime.now().minusMinutes(5)
            )

            every { queueTokenRepository.findByTokenId(tokenId) } returns token
            every { queueTokenRepository.countWaitingTokensBeforeUser("user-1", 1L, token.enteredAt) } returns 51

            val result = getQueueStatusUseCase.getQueueStatus(query)

            assertThat(result.tokenId).isEqualTo(tokenId)
            assertThat(result.userId).isEqualTo("user-1")
            assertThat(result.concertId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(QueueTokenStatus.WAITING)
            assertThat(result.position).isEqualTo(52)

            verify { queueTokenRepository.findByTokenId(tokenId) }
            verify { queueTokenRepository.countWaitingTokensBeforeUser("user-1", 1L, token.enteredAt) }
        }

        @Test
        @DisplayName("활성화된 토큰 상태 조회 성공")
        fun getActiveTokenStatus() {
            val tokenId = "active-token-id"
            val query = GetQueueStatusQuery(tokenId = tokenId)
            val token = QueueToken(
                queueTokenId = tokenId,
                userId = "user-1",
                concertId = 1L,
                tokenStatus = QueueTokenStatus.ACTIVE,
                enteredAt = LocalDateTime.now().minusMinutes(10)
            )

            every { queueTokenRepository.findByTokenId(tokenId) } returns token

            val result = getQueueStatusUseCase.getQueueStatus(query)

            assertThat(result.tokenId).isEqualTo(tokenId)
            assertThat(result.userId).isEqualTo("user-1")
            assertThat(result.concertId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(QueueTokenStatus.ACTIVE)
            assertThat(result.position).isEqualTo(0)

            verify { queueTokenRepository.findByTokenId(tokenId) }
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 실패")
        fun failToGetNonExistentToken() {
            val tokenId = "non-exist-token"
            val query = GetQueueStatusQuery(tokenId = tokenId)

            every { queueTokenRepository.findByTokenId(tokenId) } returns null

            assertThrows<QueueTokenNotFoundException> {
                getQueueStatusUseCase.getQueueStatus(query)
            }

            verify { queueTokenRepository.findByTokenId(tokenId) }
        }

        @Test
        @DisplayName("만료된 토큰 조회시 상태 업데이트")
        fun updateExpiredTokenStatus() {
            val tokenId = "expired-token-id"
            val query = GetQueueStatusQuery(tokenId = tokenId)
            val expiredToken = QueueToken(
                queueTokenId = tokenId,
                userId = "user-1",
                concertId = 1L,
                tokenStatus = QueueTokenStatus.EXPIRED,
                enteredAt = LocalDateTime.now().minusHours(1)
            )

            every { queueTokenRepository.findByTokenId(tokenId) } returns expiredToken
            every { queueTokenRepository.save(any()) } returns expiredToken

            assertThrows<InvalidTokenStatusException> {
                getQueueStatusUseCase.getQueueStatus(query)
            }

            verify { queueTokenRepository.findByTokenId(tokenId) }
            verify { queueTokenRepository.save(any()) }
        }
    }
}