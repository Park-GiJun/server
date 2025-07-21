package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock.MockConcertRepository

class ConcertRepositoryImpl(
    private val concertRepository: MockConcertRepository
) : ConcertRepository {
    override fun save(concert: Concert): Concert {
        return concertRepository.save(concert)
    }

    override fun findConcertList(): List<Concert>? {
        return concertRepository.findConcertList()
    }

    override fun findByConcertId(concertId: Long): Concert? {
        return concertRepository.findByConcertId(concertId)
    }


}