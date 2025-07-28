package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatGradeJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ConcertSeatGradeJpaRepository : JpaRepository<ConcertSeatGradeJpaEntity, Long> {

    fun findBySeatGradeAndConcertId(seatGrade: String, concertId: Long): List<ConcertSeatGradeJpaEntity>

    fun findByConcertId(concertId: Long): List<ConcertSeatGradeJpaEntity>

    @Query("SELECT csg FROM ConcertSeatGradeJpaEntity csg WHERE csg.concertId = :concertId AND csg.seatGrade = :seatGrade")
    fun findByCustomQuery(concertId: Long, seatGrade: String): List<ConcertSeatGradeJpaEntity>
}