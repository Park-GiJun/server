package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.entity.QueueTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface QueueTokenJpaRepository : JpaRepository<QueueTokenJpaEntity, String> {
    fun findByQueueTokenId(tokenId: String): QueueTokenJpaEntity?

    @Query("SELECT qt FROM QueueTokenJpaEntity qt WHERE qt.userId = :userId AND qt.concertId = :concertId AND qt.tokenStatus = 'ACTIVE'")
    fun findActiveTokenByUserIdAndConcertId(userId: String, concertId: Long): QueueTokenJpaEntity?

    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueTokenJpaEntity?

    fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int

    @Query("SELECT qt FROM QueueTokenJpaEntity qt WHERE qt.concertId = :concertId AND qt.tokenStatus = 'WAITING'")
    fun activateWaitingTokens(concertId: Long, count: Int): List<QueueTokenJpaEntity>
}