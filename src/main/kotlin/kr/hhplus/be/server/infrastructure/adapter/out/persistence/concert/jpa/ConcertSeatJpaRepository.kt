package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ConcertSeatJpaRepository : JpaRepository<ConcertSeatJpaEntity, Long> {
    fun findByConcertDateId(concertId: Long): List<ConcertSeatJpaEntity>?
}