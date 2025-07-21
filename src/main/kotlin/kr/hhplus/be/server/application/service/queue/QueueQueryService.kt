package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.QueueStatusResult
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QueueQueryService(
    private val queueTokenRepository: QueueTokenRepository
) : GetQueueStatusUseCase {
    
    // 도메인 서비스는 순수 객체로 직접 생성
    private val queueDomainService = QueueDomainService()

    override fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        // 토큰이 만료되었으면 상태를 만료로 업데이트
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

        return QueueStatusResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            status = token.tokenStatus,
            position = position
        )
    }
}