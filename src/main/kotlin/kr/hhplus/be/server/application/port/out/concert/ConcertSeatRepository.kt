package kr.hhplus.be.server.application.port.out.concert

import kr.hhplus.be.server.domain.concert.ConcertSeat

interface ConcertSeatRepository {
    fun save(concertSeat: ConcertSeat): ConcertSeat
    fun findByConcertDateId(concertDateId: Long): List<ConcertSeat>
    fun findByConcertSeatId(concertSeatId: Long): ConcertSeat?
}