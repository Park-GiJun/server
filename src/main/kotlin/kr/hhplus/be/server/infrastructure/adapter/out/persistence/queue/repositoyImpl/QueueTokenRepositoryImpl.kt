package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.repositoyImpl

import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundExceptionWithUserIdAndConcertId
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.jpa.QueueTokenJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QueueTokenRepositoryImpl(
    private val queueTokenRepository: QueueTokenJpaRepository
) : QueueTokenRepository {

    override fun save(token: QueueToken): QueueToken {
        return PersistenceMapper.toQueueTokenEntity(token)
            .let { queueTokenRepository.save(it) }
            .let { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun findByTokenId(tokenId: String): QueueToken? {
        return queueTokenRepository.findByQueueTokenId(tokenId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
            ?: throw QueueTokenNotFoundException(tokenId)
    }

    override fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        return queueTokenRepository.findActiveTokenByUserIdAndConcertId(userId, concertId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
            ?: throw QueueTokenNotFoundExceptionWithUserIdAndConcertId(userId, concertId)
    }

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        return queueTokenRepository.findByUserIdAndConcertId(userId, concertId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
            ?: throw QueueTokenNotFoundExceptionWithUserIdAndConcertId(userId, concertId)
    }

    override fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int {
        return queueTokenRepository.countWaitingTokensBeforeUser(userId, concertId, enteredAt)
    }

    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        return queueTokenRepository.activateWaitingTokens(concertId, count)
            .map { PersistenceMapper.toQueueTokenDomain(it) }
    }
}