package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.QueueStatusResult
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.exception.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QueueQueryService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueDomainService: QueueDomainService
) : GetQueueStatusUseCase {

    override fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: ${query.tokenId}")

        if (token.isExpired()) {
            token.expire()
            queueTokenRepository.save(token)
            throw InvalidTokenStatusException("Token has expired")
        }

        val position = if (token.isWaiting()) {
            val waitingCount = queueTokenRepository.countWaitingTokensBeforeUser(
                token.userId,
                token.concertId,
                token.enteredAt
            )
            queueDomainService.calculateWaitingPosition(waitingCount)
        } else 0

        val estimatedWaitTime = queueDomainService.calculateEstimatedWaitTime(position)

        return QueueStatusResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            status = token.tokenStatus,
            position = position,
            estimatedWaitTime = estimatedWaitTime
        )
    }
}