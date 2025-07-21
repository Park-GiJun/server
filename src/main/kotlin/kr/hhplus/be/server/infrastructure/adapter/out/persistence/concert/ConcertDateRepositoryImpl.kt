package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock.MockConcertDateRepository
import org.springframework.stereotype.Repository

@Repository
class ConcertDateRepositoryImpl(
    private val concertDateRepository: MockConcertDateRepository
) : ConcertDateRepository {
    override fun save(concertDate: ConcertDate): ConcertDate {
       return concertDateRepository.save(concertDate)
    }

    override fun findByConcertId(concertId: Long): List<ConcertDate> {
        return concertDateRepository.findConcertDateByConcertId(concertId)
    }

    override fun findByConcertDateId(concertDateId: Long): ConcertDate? {
        return concertDateRepository.findConcertDateByConcertDateId(concertDateId)
    }

}