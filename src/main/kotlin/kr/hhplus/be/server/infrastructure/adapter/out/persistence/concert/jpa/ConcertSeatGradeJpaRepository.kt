package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatGradeJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertSeatGradeJpaRepository : JpaRepository<ConcertSeatGradeJpaEntity, Long> {

    fun findBySeatGradeAndConcertId(seatGrade: String, concertId: Long): List<ConcertSeatGradeJpaEntity>

    fun findByConcertId(concertId: Long): List<ConcertSeatGradeJpaEntity>

}