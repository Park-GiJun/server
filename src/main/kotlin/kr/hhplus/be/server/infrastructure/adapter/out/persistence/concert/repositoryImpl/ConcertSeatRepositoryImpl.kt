package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertSeatJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class ConcertSeatRepositoryImpl(
    private val concertSeatJpaRepository: ConcertSeatJpaRepository
) : ConcertSeatRepository {

    override fun save(concertSeat: ConcertSeat): ConcertSeat {
        return PersistenceMapper.toConcertSeatEntity(concertSeat)
            .let { concertSeatJpaRepository.save(it) }
            .let { PersistenceMapper.toConcertSeatDomain(it) }
    }

    override fun findByConcertDateId(concertDateId: Long): List<ConcertSeat> {
        return concertSeatJpaRepository.findByConcertDateId(concertDateId)
            ?.map { PersistenceMapper.toConcertSeatDomain(it) }
            ?: emptyList()
    }

    override fun findByConcertSeatId(concertSeatId: Long): ConcertSeat? {
        return concertSeatJpaRepository.findById(concertSeatId)
            .map { PersistenceMapper.toConcertSeatDomain(it) }
            .orElse(null)
    }

    override fun findByConcertSeatIdWithLock(concertSeatId: Long): ConcertSeat? {
        return concertSeatJpaRepository.findByConcertSeatIdWithLock(concertSeatId)
            ?.let { PersistenceMapper.toConcertSeatDomain(it) }
    }
}