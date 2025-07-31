package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.repositoryImpl

import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertSeatGradeJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class ConcertSeatGradeRepositoryImpl(
    private val concertSeatGradeJpaRepository: ConcertSeatGradeJpaRepository
) : ConcertSeatGradeRepository {

    override fun save(concertSeatGrade: ConcertSeatGrade): ConcertSeatGrade {
        return PersistenceMapper.toConcertSeatGradeEntity(concertSeatGrade)
            .let { concertSeatGradeJpaRepository.save(it) }
            .let { PersistenceMapper.toConcertSeatGradeDomain(it) }
    }

    override fun findBySeatGrade(seatGrade: String, concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGradeJpaRepository.findBySeatGradeAndConcertId(seatGrade, concertId)
            .map { PersistenceMapper.toConcertSeatGradeDomain(it) }
    }

    override fun findByConcertId(concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGradeJpaRepository.findByConcertId(concertId)
            .map { PersistenceMapper.toConcertSeatGradeDomain(it) }
    }

    override fun findById(id: Long): ConcertSeatGrade? {
        return concertSeatGradeJpaRepository.findById(id)
            .map { PersistenceMapper.toConcertSeatGradeDomain(it) }
            .orElse(null)
    }
}