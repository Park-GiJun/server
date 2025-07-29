package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertSeatJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class ConcertSeatRepositoryImpl(
    private val concertSeatRepository: ConcertSeatJpaRepository
) : ConcertSeatRepository {
    override fun save(concertSeat: ConcertSeat): ConcertSeat {
        return PersistenceMapper.toConcertSeatEntity(concertSeat)
            .let { concertSeatRepository.save(it) }
            .let { PersistenceMapper.toConcertSeatDomain(it) }
    }

    override fun findByConcertDateId(concertDateId: Long): List<ConcertSeat> {
        return  concertSeatRepository.findByConcertDateId(concertDateId)
            ?.map { PersistenceMapper.toConcertSeatDomain(it) }
            ?: throw ConcertSeatNotFoundException(concertDateId)
    }

    override fun findByConcertSeatId(concertSeatId: Long): ConcertSeat? {
        return concertSeatRepository.findById(concertSeatId)
            .map { PersistenceMapper.toConcertSeatDomain(it) }
            .orElse(null)
    }
}