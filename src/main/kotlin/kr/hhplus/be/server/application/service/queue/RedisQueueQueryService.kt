package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.dto.queue.GetActivateTokensCountQuery
import kr.hhplus.be.server.application.dto.queue.GetActivateTokensCountResult
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusResult
import kr.hhplus.be.server.application.port.`in`.queue.GetActivateTokensCountUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RedisQueueQueryService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueManagementService: RedisQueueManagementService,
    private val redisQueueDomainService: RedisQueueDomainService
) : GetQueueStatusUseCase, GetActivateTokensCountUseCase {

    private val log = LoggerFactory.getLogger(RedisQueueQueryService::class.java)

    override fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult {
        log.info("Redis 대기열 상태 조회: tokenId=${query.tokenId}")

        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        if (token.isExpired()) {
            log.warn("만료된 토큰 상태 조회: tokenId=${query.tokenId}")
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        val position = if (token.isWaiting()) {
            val rank = queueManagementService.getWaitingPosition(token.concertId, token.userId)
            redisQueueDomainService.calculateWaitingPosition(rank)
        } else 0

        val estimatedWaitTime = calculateEstimatedWaitTime(position)

        log.info("Redis 대기열 상태 조회 완료: tokenId=${query.tokenId}, 상태=${token.tokenStatus}, 순서=$position")

        return GetQueueStatusResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            status = token.tokenStatus,
            concertId = token.concertId,
            position = position,
            estimatedWaitTime = estimatedWaitTime
        )
    }

    override fun getActivateTokensCountByQueue(query: GetActivateTokensCountQuery): GetActivateTokensCountResult {
        log.info("Redis 콘서트 활성 토큰 수 조회: concertId=${query.concertId}")

        val stats = queueManagementService.getQueueStats(query.concertId)

        log.info("Redis 콘서트 활성 토큰 수 조회 완료: concertId=${query.concertId}, 활성토큰=${stats.activeCount}개")

        return GetActivateTokensCountResult(
            concertId = query.concertId,
            activeCount = stats.activeCount.toInt()
        )
    }

    /**
     * 예상 대기 시간 계산 (분 단위)
     */
    private fun calculateEstimatedWaitTime(position: Int): Int {
        return if (position > 0) {
            // 10명당 1분 대기로 가정
            (position / 10) * 60
        } else 0
    }
}