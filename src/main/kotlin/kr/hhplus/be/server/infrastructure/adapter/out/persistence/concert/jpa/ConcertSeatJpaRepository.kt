package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertSeatJpaRepository : JpaRepository<ConcertSeatJpaEntity, Long> {
}