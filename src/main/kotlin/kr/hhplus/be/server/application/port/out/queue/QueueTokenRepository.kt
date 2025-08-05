package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import java.time.LocalDateTime

interface QueueTokenRepository {
    fun save(token: QueueToken): QueueToken
    fun findByTokenId(tokenId: String): QueueToken?
    fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken?
    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken?
    fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int
    fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken>
    fun countActiveTokensByConcert(concertId: Long): Int
    fun findWaitingTokensByConcert(concertId: Long): List<QueueToken>
}