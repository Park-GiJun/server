package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockQueueTokenRepository {

    private val log = LoggerFactory.getLogger(MockQueueTokenRepository::class.java)
    private val queueTokens = ConcurrentHashMap<String, QueueToken>()

    fun save(queueToken: QueueToken): QueueToken {
        queueTokens[queueToken.queueTokenId] = queueToken
        log.info("Saved queue token: tokenId=${queueToken.queueTokenId}, userId=${queueToken.userId}, concertId=${queueToken.concertId}, status=${queueToken.tokenStatus}")
        return queueToken
    }

    fun findByTokenId(tokenId: String): QueueToken? {
        return queueTokens[tokenId]
    }

    fun findByQueueToken(queueToken: String): QueueToken? {
        return queueTokens[queueToken]
    }

    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        return queueTokens.values.find {
            it.userId == userId && it.concertId == concertId
        }
    }

    fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        return queueTokens.values.find {
            it.userId == userId &&
                    it.concertId == concertId &&
                    (it.isActive() || it.isWaiting())
        }
    }

    fun findWaitingTokensByConcertIdOrderByEnteredAt(concertId: Long): List<QueueToken> {
        return queueTokens.values
            .filter { it.concertId == concertId && it.tokenStatus == QueueTokenStatus.WAITING }
            .sortedBy { it.enteredAt }
    }

    fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Long {
        return queueTokens.values.count {
            it.concertId == concertId &&
                    it.tokenStatus == QueueTokenStatus.WAITING &&
                    it.enteredAt.isBefore(enteredAt)
        }.toLong()
    }

    fun updateTokenStatus(tokenId: String, status: QueueTokenStatus): QueueToken? {
        val token = queueTokens[tokenId] ?: return null
        token.tokenStatus = status
        queueTokens[tokenId] = token
        log.info("Updated token status: tokenId=$tokenId, status=$status")
        return token
    }

    fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        val waitingTokens = findWaitingTokensByConcertIdOrderByEnteredAt(concertId)
            .take(count)

        return waitingTokens.map { token ->
            token.activate()
            queueTokens[token.queueTokenId] = token
            log.info("Activated token: tokenId=${token.queueTokenId}, userId=${token.userId}")
            token
        }
    }

    fun findAll(): List<QueueToken> {
        return queueTokens.values.toList()
    }

    fun deleteByQueueToken(queueToken: String): Boolean {
        return queueTokens.remove(queueToken) != null
    }

    fun deleteByTokenId(tokenId: String): Boolean {
        return queueTokens.remove(tokenId) != null
    }

    fun clear() {
        queueTokens.clear()
    }
}