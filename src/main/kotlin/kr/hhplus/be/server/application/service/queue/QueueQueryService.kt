package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.mapper.QueueMapper
import kr.hhplus.be.server.application.port.`in`.queue.GetActivateTokensCountByConcert
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QueueQueryService(
    private val queueTokenRepository: QueueTokenRepository
) : GetQueueStatusUseCase, GetActivateTokensCountByConcert {

    private val queueDomainService = QueueDomainService()

    override fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        if (token.isExpired()) {
            val expiredToken = token.expire()
            queueTokenRepository.save(expiredToken)
            throw InvalidTokenStatusException(token.tokenStatus, kr.hhplus.be.server.domain.queue.QueueTokenStatus.ACTIVE)
        }

        val position = if (token.isWaiting()) {
            val waitingCount = queueTokenRepository.countWaitingTokensBeforeUser(
                token.userId,
                token.concertId,
                token.enteredAt
            )
            queueDomainService.calculateWaitingPosition(waitingCount)
        } else 0

        return QueueMapper.toStatusResult(token, position)
    }

    override fun getActivateTokensCountByQueue(concertId : Long): Int {
        return queueTokenRepository.countActiveTokensByConcert(concertId)
    }
}