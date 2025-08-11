package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.port.`in`.queue.GetActivateTokensCountByConcert
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RedisQueueQueryService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueManagementService: RedisQueueManagementService,
    private val redisQueueDomainService: RedisQueueDomainService
) : GetQueueStatusUseCase, GetActivateTokensCountByConcert {

    private val log = LoggerFactory.getLogger(RedisQueueQueryService::class.java)

    override fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
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

        log.info("Redis 대기열 상태 조회: tokenId=${query.tokenId}, 상태=${token.tokenStatus}, 순서=$position")

        return QueueStatusResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            status = token.tokenStatus,
            concertId = token.concertId,
            position = position
        )
    }

    override fun getActivateTokensCountByQueue(concertId: Long): Int {
        val stats = queueManagementService.getQueueStats(concertId)
        log.info("Redis 콘서트 활성 토큰 수 조회: concertId=$concertId, 활성토큰=${stats.activeCount}개")
        return stats.activeCount.toInt()
    }

    private fun calculateEstimatedWaitTime(position: Int): Int {
        return if (position > 0) (position / 10) * 60 else 0
    }
}