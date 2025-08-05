package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.jpa

import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.entity.QueueTokenJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

interface QueueTokenJpaRepository : JpaRepository<QueueTokenJpaEntity, String> {
    fun findByQueueTokenId(tokenId: String): QueueTokenJpaEntity?

    @Query("SELECT qt FROM QueueTokenJpaEntity qt WHERE qt.userId = :userId AND qt.concertId = :concertId AND qt.tokenStatus = 'ACTIVE'")
    fun findActiveTokenByUserIdAndConcertId(userId: String, concertId: Long): QueueTokenJpaEntity?

    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueTokenJpaEntity?

    @Query("""
        SELECT COUNT(qt) FROM QueueTokenJpaEntity qt 
        WHERE qt.concertId = :concertId 
        AND qt.tokenStatus = 'WAITING' 
        AND qt.enteredAt < :enteredAt
    """)
    fun countWaitingTokensBeforeUser(
        @Param("userId") userId: String,
        @Param("concertId") concertId: Long,
        @Param("enteredAt") enteredAt: LocalDateTime
    ): Int

    @Query("""
        SELECT qt FROM QueueTokenJpaEntity qt 
        WHERE qt.concertId = :concertId AND qt.tokenStatus = 'WAITING' 
        ORDER BY qt.enteredAt ASC
    """)
    fun findWaitingTokensByConcertIdOrderByEnteredAt(
        @Param("concertId") concertId: Long,
        pageable: Pageable
    ): List<QueueTokenJpaEntity>

    @Query("""
        SELECT qt FROM QueueTokenJpaEntity qt 
        WHERE qt.concertId = :concertId AND qt.tokenStatus = 'WAITING' 
        ORDER BY qt.enteredAt ASC
    """)
    fun findWaitingTokensByConcertIdOrderByEnteredAtAll(
        @Param("concertId") concertId: Long
    ): List<QueueTokenJpaEntity>

    @Modifying
    @Query("""
        UPDATE QueueTokenJpaEntity qt 
        SET qt.tokenStatus = 'ACTIVE'
        WHERE qt.queueTokenId IN :tokenIds
    """)
    fun updateTokensToActive(@Param("tokenIds") tokenIds: List<String>): Int

    fun countByConcertIdAndTokenStatus(concertId: Long, status: QueueTokenStatus): Int

    @Query("SELECT COUNT(qt) FROM QueueTokenJpaEntity qt WHERE qt.concertId = :concertId AND qt.tokenStatus = 'ACTIVE'")
    fun countActiveByConcertId(@Param("concertId") concertId: Long): Int
}