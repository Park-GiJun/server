package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ConcertSeatJpaRepository : JpaRepository<ConcertSeatJpaEntity, Long> {
    fun findByConcertDateId(concertId: Long): List<ConcertSeatJpaEntity>?

    @Query(
        value = "SELECT * FROM concert_seat WHERE concert_seat_id = :concertSeatId FOR UPDATE NOWAIT",
        nativeQuery = true
    )
    fun findByConcertSeatIdWithLock(@Param("concertSeatId") seatId: Long): ConcertSeatJpaEntity?
}