package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus

interface QueueTokenRepository {

    fun save(token: QueueToken): QueueToken

    fun findByTokenId(tokenId: String): QueueToken?

    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken?

    fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken>

    fun findWaitingTokensByConcert(concertId: Long, limit: Int = 50): List<QueueToken>

    fun findActiveTokensByConcert(concertId: Long): List<QueueToken>

    fun findExpiredTokensByConcert(concertId: Long? = null): List<QueueToken>

    fun updateTokenStatus(tokenId: String, status: QueueTokenStatus): QueueToken?

    fun deleteToken(tokenId: String): Boolean

    fun countTokensByStatusAndConcert(concertId: Long): Map<QueueTokenStatus, Int>

    fun expireActiveToken(tokenId: String, reason: String = "시간 만료"): QueueToken?

    fun hasActiveToken(userId: String, concertId: Long): Boolean

    fun batchUpdateTokenStatus(tokenIds: List<String>, status: QueueTokenStatus): Int
}