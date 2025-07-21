package kr.hhplus.be.server.application.port.out.concert

import kr.hhplus.be.server.domain.concert.ConcertDate

interface ConcertDateRepository {
    fun save(concertDate: ConcertDate): ConcertDate
    fun findByConcertId(concertId: Long): List<ConcertDate>
    fun findByConcertDateId(concertDateId: Long): ConcertDate?
}