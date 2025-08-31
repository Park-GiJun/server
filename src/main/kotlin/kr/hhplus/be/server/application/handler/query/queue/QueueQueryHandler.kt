package kr.hhplus.be.server.application.handler.query.queue

import kr.hhplus.be.server.application.dto.queue.query.GetActivateTokensCountQuery
import kr.hhplus.be.server.application.dto.queue.result.GetActivateTokensCountResult
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.GetQueueStatusResult
import kr.hhplus.be.server.application.port.`in`.queue.GetActivateTokensCountUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QueueQueryHandler(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueManagementService: RedisQueueManagementService,
    private val redisQueueDomainService: RedisQueueDomainService
) : GetQueueStatusUseCase, GetActivateTokensCountUseCase {

    private val log = LoggerFactory.getLogger(QueueQueryHandler::class.java)

    override fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult {
        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        if (token.isExpired()) {
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        val position = if (token.isWaiting()) {
            val rank = queueManagementService.getWaitingPosition(token.concertId, token.userId)
            redisQueueDomainService.calculateWaitingPosition(rank)
        } else 0

        val estimatedWaitTime = calculateEstimatedWaitTime(position)
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
        val stats = queueManagementService.getQueueStats(query.concertId)
        return GetActivateTokensCountResult(
            concertId = query.concertId,
            activeCount = stats.activeCount.toInt()
        )
    }

    private fun calculateEstimatedWaitTime(position: Int): Int {
        return if (position > 0) {
            (position / 10) * 60
        } else 0
    }
}