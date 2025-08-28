package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.dto.concert.result.PopularConcertResult
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.ProjectionMapper
import org.springframework.stereotype.Component

@Component
class ConcertRepositoryImpl(
    private val concertJpaRepository: ConcertJpaRepository
) : ConcertRepository {

    override fun save(concert: Concert): Concert {
        return PersistenceMapper.toConcertEntity(concert)
            .let { concertJpaRepository.save(it) }
            .let { PersistenceMapper.toConcertDomain(it) }
    }

    override fun findConcertList(): List<Concert> {
        return concertJpaRepository.findAll()
            .map { PersistenceMapper.toConcertDomain(it) }
    }

    override fun findByConcertId(concertId: Long): Concert? {
        return concertJpaRepository.findByConcertId(concertId)
            ?.let { PersistenceMapper.toConcertDomain(it) }
    }

    override fun findByPopularConcert(limit: Int): List<PopularConcertResult> {
        return concertJpaRepository.findPopularConcertsLast5Minutes(limit)
            .map { ProjectionMapper.popularConcertToDto(it) }
    }
}