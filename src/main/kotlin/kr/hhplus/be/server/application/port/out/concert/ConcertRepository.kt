package kr.hhplus.be.server.application.port.out.concert

import kr.hhplus.be.server.domain.concert.Concert

interface ConcertRepository {

    fun save(concert: Concert) : Concert
    fun findConcertList() : List<Concert>
    fun findByConcertId(concertId: Long): Concert?
}