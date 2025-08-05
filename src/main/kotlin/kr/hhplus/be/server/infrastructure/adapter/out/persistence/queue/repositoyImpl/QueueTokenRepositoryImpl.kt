package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.repositoyImpl

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.jpa.QueueTokenJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Component
class QueueTokenRepositoryImpl(
    private val queueTokenJpaRepository: QueueTokenJpaRepository
) : QueueTokenRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(token: QueueToken): QueueToken {
        return PersistenceMapper.toQueueTokenEntity(token)
            .let { queueTokenJpaRepository.save(it) }
            .let { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun findByTokenId(tokenId: String): QueueToken? {
        return queueTokenJpaRepository.findByQueueTokenId(tokenId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        return queueTokenJpaRepository.findActiveTokenByUserIdAndConcertId(userId, concertId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        return queueTokenJpaRepository.findByUserIdAndConcertId(userId, concertId)
            ?.let { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int {
        return queueTokenJpaRepository.countWaitingTokensBeforeUser(userId, concertId, enteredAt)
    }

    override fun findWaitingTokensByConcert(concertId: Long): List<QueueToken> {
        val sort = Sort.by(Sort.Direction.ASC, "enteredAt")

        return queueTokenJpaRepository.findByConcertIdAndTokenStatus(
            concertId = concertId,
            tokenStatus = QueueTokenStatus.WAITING,
            sort = sort
        ).map { PersistenceMapper.toQueueTokenDomain(it) }
    }

    override fun countActiveTokensByConcert(concertId: Long): Int {
        return queueTokenJpaRepository.countByConcertIdAndTokenStatus(concertId, QueueTokenStatus.ACTIVE)
    }

    @Transactional
    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        val waitingTokens = queueTokenJpaRepository.findWaitingTokensByConcertIdOrderByEnteredAt(
            concertId,
            PageRequest.of(0, count)
        )

        if (waitingTokens.isEmpty()) {
            return emptyList()
        }

        val tokenIds = waitingTokens.map { it.queueTokenId }
        queueTokenJpaRepository.updateTokensToActive(tokenIds)

        return waitingTokens.onEach { it.tokenStatus = QueueTokenStatus.ACTIVE }
            .map { PersistenceMapper.toQueueTokenDomain(it) }
    }
}