package kr.hhplus.be.server.repository.mock

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
        queueTokens[queueToken.queueToken] = queueToken
        log.info("Saved queue token: userId=${queueToken.userId}, concertId=${queueToken.concertId}, tokenId=${queueToken.queueToken}, status=${queueToken.tokenStatus}")
        return queueToken
    }

    fun findByQueueToken(queueToken: String): QueueToken? {
        return queueTokens[queueToken]
    }

    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        return queueTokens.values.find {
            it.userId == userId && it.concertId == concertId
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

    fun findAll(): List<QueueToken> {
        return queueTokens.values.toList()
    }

    fun deleteByQueueToken(queueToken: String): Boolean {
        return queueTokens.remove(queueToken) != null
    }

    fun clear() {
        queueTokens.clear()
    }
}