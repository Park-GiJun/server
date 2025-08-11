package kr.hhplus.be.server.domain.queue

import org.springframework.stereotype.Component

/**
 * 대기열 도메인 서비스
 * - 순수한 비즈니스 로직만 포함
 */
@Component
class QueueDomainService {

    companion object {
        private const val MAX_ACTIVE_TOKENS_PER_CONCERT = 100
        private const val ESTIMATED_PROCESSING_TIME_MINUTES = 5
    }

    /**
     * 활성화 가능한 토큰 수 계산
     * @param currentActiveCount 현재 활성 토큰 수
     * @param maxActiveTokens 최대 허용 활성 토큰 수
     * @return 추가로 활성화 가능한 수
     */
    fun calculateActivationCapacity(
        currentActiveCount: Int,
        maxActiveTokens: Int = MAX_ACTIVE_TOKENS_PER_CONCERT
    ): Int {
        require(currentActiveCount >= 0) { "현재 활성 토큰 수는 0 이상이어야 합니다" }
        require(maxActiveTokens > 0) { "최대 활성 토큰 수는 1 이상이어야 합니다" }

        return maxOf(0, maxActiveTokens - currentActiveCount)
    }

    /**
     * 예상 대기 시간 계산
     * @param position 현재 대기열 위치 (0부터 시작)
     * @return 예상 대기 시간 (분)
     */
    fun calculateEstimatedWaitTime(position: Long): Int {
        require(position >= 0) { "대기열 위치는 0 이상이어야 합니다" }

        return (position * ESTIMATED_PROCESSING_TIME_MINUTES).toInt()
    }

    /**
     * 토큰 활성화 가능 여부 확인
     * @param token 확인할 토큰
     * @param currentPosition 현재 대기열 위치
     * @return 활성화 가능 여부
     */
    fun canActivateToken(token: QueueToken, currentPosition: Long): Boolean {
        return token.isWaiting() && currentPosition == 0L
    }

    /**
     * 대기열 진입 검증
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param existingToken 기존 토큰 (있는 경우)
     * @return 진입 가능 여부
     */
    fun validateQueueEntry(
        userId: String,
        concertId: Long,
        existingToken: QueueToken?
    ): QueueEntryValidation {
        // 기존 활성 토큰이 있으면 재사용
        if (existingToken?.isActive() == true) {
            return QueueEntryValidation.ExistingActive(existingToken)
        }

        // 기존 대기 토큰이 있으면 재사용
        if (existingToken?.isWaiting() == true) {
            return QueueEntryValidation.ExistingWaiting(existingToken)
        }

        // 새 토큰 생성 필요
        return QueueEntryValidation.CreateNew
    }

    /**
     * 대기열 정리 대상 식별
     * @param tokens 토큰 목록
     * @return 정리 대상 토큰들
     */
    fun identifyExpiredTokens(tokens: List<QueueToken>): List<QueueToken> {
        return tokens.filter { token ->
            when (token.status) {
                QueueTokenStatus.EXPIRED -> true
                QueueTokenStatus.COMPLETED -> {
                    // 완료 후 1시간이 지난 토큰들
                    token.activatedAt?.plusHours(1)?.isBefore(java.time.LocalDateTime.now()) == true
                }
                else -> false
            }
        }
    }
}

/**
 * 대기열 진입 검증 결과
 */
sealed class QueueEntryValidation {
    object CreateNew : QueueEntryValidation()
    data class ExistingActive(val token: QueueToken) : QueueEntryValidation()
    data class ExistingWaiting(val token: QueueToken) : QueueEntryValidation()
}