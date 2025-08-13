package kr.hhplus.be.server._disable_mock.queue.mock

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

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

    fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int {
        return queueTokens.values.count {
            it.concertId == concertId &&
                    it.tokenStatus == QueueTokenStatus.WAITING &&
                    it.enteredAt.isBefore(enteredAt)
        }
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
}