package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class ConcertRepositoryImpl(
    private val concertRepository: ConcertJpaRepository
) : ConcertRepository {

    override fun save(concert: Concert): Concert {
        return PersistenceMapper.toConcertEntity(concert)
            .let { concertRepository.save(it) }
            .let { PersistenceMapper.toConcertDomain(it) }
    }

    override fun findConcertList(): List<Concert> {
        return concertRepository.findAll()
            .map { PersistenceMapper.toConcertDomain(it) }
    }

    override fun findByConcertId(concertId: Long): Concert? {
        return concertRepository.findByConcertId(concertId)
            ?.let { PersistenceMapper.toConcertDomain(it) }
    }
}