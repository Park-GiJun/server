package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertDateJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class ConcertDateRepositoryImpl(
    private val concertDateJpaRepository: ConcertDateJpaRepository
) : ConcertDateRepository {

    override fun save(concertDate: ConcertDate): ConcertDate {
        return PersistenceMapper.toConcertDateEntity(concertDate)
            .let { concertDateJpaRepository.save(it) }
            .let { PersistenceMapper.toConcertDateDomain(it) }
    }

    override fun findByConcertId(concertId: Long): List<ConcertDate> {
        return concertDateJpaRepository.findByConcertId(concertId)
            ?.map { PersistenceMapper.toConcertDateDomain(it) }
            ?: emptyList()
    }

    override fun findByConcertDateId(concertDateId: Long): ConcertDate? {
        return concertDateJpaRepository.findByConcertDateId(concertDateId)
            ?.let { PersistenceMapper.toConcertDateDomain(it) }
    }
}