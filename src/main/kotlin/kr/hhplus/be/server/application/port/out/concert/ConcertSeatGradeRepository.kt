package kr.hhplus.be.server.application.port.out.concert

import kr.hhplus.be.server.domain.concert.ConcertSeatGrade

interface ConcertSeatGradeRepository {
    fun save(concertSeatGrade: ConcertSeatGrade): ConcertSeatGrade
    fun findByConcertId(concertId: Long): List<ConcertSeatGrade>
    fun findBySeatGradeAndConcertId(seatGrade: String, concertId: Long): List<ConcertSeatGrade>
}