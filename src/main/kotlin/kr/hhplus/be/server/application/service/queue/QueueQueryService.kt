package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.mapper.QueueMapper
import kr.hhplus.be.server.application.port.`in`.queue.GetActivateTokensCountByConcert
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.domain.queue.service.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QueueQueryService(
    private val queueTokenRepository: QueueTokenRepository
) : GetQueueStatusUseCase, GetActivateTokensCountByConcert {

    private val log = LoggerFactory.getLogger(QueueQueryService::class.java)
    private val queueDomainService = QueueDomainService()

    override fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
        val token = queueTokenRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        if (token.isExpired()) {
            log.warn("만료된 토큰 상태 조회: tokenId=${query.tokenId}")
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

        log.info("대기열 상태 조회: tokenId=${query.tokenId}, 상태=${token.tokenStatus}, 순서=${position}")
        return QueueMapper.toStatusResult(token, position)
    }

    override fun getActivateTokensCountByQueue(concertId: Long): Int {
        val activeCount = queueTokenRepository.countActiveTokensByConcert(concertId)
        log.info("콘서트 활성 토큰 수 조회: concertId=${concertId}, 활성토큰=${activeCount}개")
        return activeCount
    }
}