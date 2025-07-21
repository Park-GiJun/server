package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock.MockConcertSeatRepository
import org.springframework.stereotype.Repository

@Repository
class ConcertSeatRepositoryImpl(
    private val concertSeatRepository: MockConcertSeatRepository
) : ConcertSeatRepository {
    override fun save(concertSeat: ConcertSeat): ConcertSeat {
        return concertSeatRepository.save(concertSeat)
    }

    override fun findByConcertDateId(concertDateId: Long): List<ConcertSeat> {
        return concertSeatRepository.findConcertSeats(concertDateId) ?: emptyList()
    }

    override fun findByConcertSeatId(concertSeatId: Long): ConcertSeat? {
        return concertSeatRepository.findByConcertSeatId(concertSeatId)
    }

}