package kr.hhplus.be.server.domain.queue.service

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.TokenExpiredException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID


/**
 * Redis용 도메인 서비스
 * - 비즈니스 로직만 포함 및 기존 코드 리펙토링
 */
class QueueDomainService {

    private val log = LoggerFactory.getLogger(QueueDomainService::class.java)

    companion object {
        private const val MAX_ACTIVE_TOKENS_PER_CONCERT = 100
        private const val MAX_QUEUE_WAITING_TIME_HOURS = 24L
    }

    /**
     * 대기열 계산
     * - 입장순서 기준
     * - 동일한 콘서트에서만 순서 적용
     *
     * @param allWaitingTokens 해당 콘서트의 모든 대기중인 토큰
     * @param targetToken 위치 계산할 대상 토큰
     * @return 대기열 위치 (0부터 시작)
     */
    fun calculateQueuePosition(
        allWaitingTokens: List<QueueToken>,
        targetToken: QueueToken
    ): Long {
        require(targetToken.tokenStatus == QueueTokenStatus.WAITING) {
            log.info("대기중인 토큰만 위치 계산이 가능합니다.")
        }

        val sameContentTokens = allWaitingTokens
            .filter { it.concertId == targetToken.concertId }
            .filter { it.tokenStatus == QueueTokenStatus.WAITING }
            .sortedBy { it.enteredAt }

        val position = sameContentTokens.indexOfFirst { it.queueTokenId == targetToken.queueTokenId }

        return if (position >= 0) position.toLong() else Long.MAX_VALUE
    }
}