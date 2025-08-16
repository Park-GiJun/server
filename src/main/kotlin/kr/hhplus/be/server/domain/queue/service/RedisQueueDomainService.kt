package kr.hhplus.be.server.domain.queue.service

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Component
class RedisQueueDomainService {

    companion object {
        const val MAX_ACTIVE_USERS_PER_CONCERT = 100
        const val TOKEN_TTL_HOURS = 24L
        const val ACTIVE_SESSION_TTL_MINUTES = 30L
    }

    /**
     * Redis에 최적화된 토큰 생성
     */
    fun createNewQueueToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = generateTokenId(userId, concertId),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.WAITING,
            enteredAt = LocalDateTime.now(),
            expiresAt = LocalDateTime.now().plusHours(TOKEN_TTL_HOURS)
        )
    }

    /**
     * 토큰 ID 생성 (Redis Key 최적화)
     */
    private fun generateTokenId(userId: String, concertId: Long): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return "qt_${concertId}_${userId}_${timestamp}_${uuid.take(8)}"
    }

    /**
     * 활성화 가능한 토큰 수 계산
     */
    fun calculateTokensToActivate(currentActiveCount: Int): Int {
        val availableSlots = MAX_ACTIVE_USERS_PER_CONCERT - currentActiveCount
        return if (availableSlots > 0) minOf(availableSlots, 10) else 0
    }

    /**
     * 대기열 위치 계산 (Redis 기반)
     */
    fun calculateWaitingPosition(rank: Long): Int {
        return if (rank >= 0) (rank + 1).toInt() else 0
    }

    /**
     * 토큰 상태 전환 검증
     */
    fun validateStatusTransition(
        currentStatus: QueueTokenStatus,
        targetStatus: QueueTokenStatus
    ): Boolean {
        return when (currentStatus) {
            QueueTokenStatus.WAITING -> targetStatus in listOf(
                QueueTokenStatus.ACTIVE,
                QueueTokenStatus.EXPIRED
            )
            QueueTokenStatus.ACTIVE -> targetStatus in listOf(
                QueueTokenStatus.COMPLETED,
                QueueTokenStatus.EXPIRED
            )
            else -> false
        }
    }

    /**
     * TTL 계산
     */
    fun calculateTTL(status: QueueTokenStatus): Duration {
        return when (status) {
            QueueTokenStatus.WAITING -> Duration.ofHours(TOKEN_TTL_HOURS)
            QueueTokenStatus.ACTIVE -> Duration.ofMinutes(ACTIVE_SESSION_TTL_MINUTES)
            QueueTokenStatus.COMPLETED -> Duration.ofMinutes(5)
            QueueTokenStatus.EXPIRED -> Duration.ofMinutes(1)
        }
    }
}