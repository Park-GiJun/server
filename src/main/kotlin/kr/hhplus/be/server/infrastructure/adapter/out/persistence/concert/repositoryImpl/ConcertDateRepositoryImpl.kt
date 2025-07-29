package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.domain.concert.exception.ConcertDateNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertDateJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class ConcertDateRepositoryImpl(
    private val concertDateRepository: ConcertDateJpaRepository
) : ConcertDateRepository {

    override fun save(concertDate: ConcertDate): ConcertDate {
        return PersistenceMapper.toConcertDateEntity(concertDate)
            .let { concertDateRepository.save(it) }
            .let { PersistenceMapper.toConcertDateDomain(it) }
    }

    override fun findByConcertId(concertId: Long): List<ConcertDate> {
        return concertDateRepository.findByConcertId(concertId)
            ?.map { PersistenceMapper.toConcertDateDomain(it) }
            ?: throw ConcertDateNotFoundException(concertId)
    }

    override fun findByConcertDateId(concertDateId: Long): ConcertDate? {
        return concertDateRepository.findByConcertDateId(concertDateId)
            ?.let { PersistenceMapper.toConcertDateDomain(it) }
    }
}