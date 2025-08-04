package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface ConcertSeatJpaRepository : JpaRepository<ConcertSeatJpaEntity, Long> {
    fun findByConcertDateId(concertId: Long): List<ConcertSeatJpaEntity>?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ConcertSeatJpaEntity cs WHERE cs.concertSeatId = :seatId")
    fun findByConcertSeatIdWithLock(@Param("seatId") seatId: Long): ConcertSeatJpaEntity?
}