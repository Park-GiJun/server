package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ConcertJpaRepository : JpaRepository<ConcertJpaEntity, Long> {
    fun findByConcertId(concertId: Long) : ConcertJpaEntity?
}