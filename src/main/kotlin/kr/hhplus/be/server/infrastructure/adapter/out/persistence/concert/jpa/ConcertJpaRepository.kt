package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.projection.PopularConcertProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ConcertJpaRepository : JpaRepository<ConcertJpaEntity, Long> {
    fun findByConcertId(concertId: Long): ConcertJpaEntity?

    @Query(
        value = """
            SELECT 
                c.concert_id AS concertId,
                c.concert_name AS concertName,
                COUNT(*) AS reservedCount
            FROM concerts c
            JOIN concert_date cd 
                ON c.concert_id = cd.concert_id
            JOIN concert_seat cs 
                ON cd.concert_date_id = cs.concert_date_id
            WHERE cs.created_at >= NOW() - INTERVAL 5 MINUTE
              AND cs.seat_status = 'RESERVED'
            GROUP BY c.concert_id, c.concert_name
            ORDER BY reservedCount DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findPopularConcertsLast5Minutes(limit: Int): List<PopularConcertProjection>
}