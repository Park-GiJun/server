package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertDateJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ConcertDateJpaRepository : JpaRepository<ConcertDateJpaEntity, Long> {
    fun findByConcertId(concertId: Long): List<ConcertDateJpaEntity>?

    fun findByConcertDateId(concertDateId: Long): ConcertDateJpaEntity?
}