package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface ConcertSeatJpaRepository : JpaRepository<ConcertSeatJpaEntity, Long> {
    fun findByConcertDateId(concertId: Long): List<ConcertSeatJpaEntity>?

    @Query(
        value = "SELECT * FROM concert_seat WHERE concert_seat_id = :seatId FOR UPDATE NOWAIT",
        nativeQuery = true
    )
    fun findByConcertSeatIdWithLock(@Param("seatId") seatId: Long): ConcertSeatJpaEntity?
}