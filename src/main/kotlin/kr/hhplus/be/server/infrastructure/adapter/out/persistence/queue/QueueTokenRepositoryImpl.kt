package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue

import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.repository.mock.MockQueueTokenRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QueueTokenRepositoryImpl(
    private val mockQueueTokenRepository: MockQueueTokenRepository
) : QueueTokenRepository {

    override fun save(token: QueueToken): QueueToken {
        return mockQueueTokenRepository.save(token)
    }

    override fun findByTokenId(tokenId: String): QueueToken? {
        return mockQueueTokenRepository.findByTokenId(tokenId)
    }

    override fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        return mockQueueTokenRepository.findActiveTokenByUserAndConcert(userId, concertId)
    }

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        return mockQueueTokenRepository.findByUserIdAndConcertId(userId, concertId)
    }

    override fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Long {
        return mockQueueTokenRepository.countWaitingTokensBeforeUser(userId, concertId, enteredAt)
    }

    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        return mockQueueTokenRepository.activateWaitingTokens(concertId, count)
    }
}