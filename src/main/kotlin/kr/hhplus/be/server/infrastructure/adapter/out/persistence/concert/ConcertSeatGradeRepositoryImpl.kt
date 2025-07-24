package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock.MockConcertSeatGradeRepository
import org.springframework.stereotype.Repository

@Repository
class ConcertSeatGradeRepositoryImpl(
    private val concertSeatGradeRepository: MockConcertSeatGradeRepository
) : ConcertSeatGradeRepository {
    override fun save(concertSeatGrade: ConcertSeatGrade): ConcertSeatGrade {
        return concertSeatGradeRepository.save(concertSeatGrade)
    }

    override fun findBySeatGrade(
        seatGrade: String,
        concertId: Long
    ): List<ConcertSeatGrade> {
        return concertSeatGradeRepository.findBySeatGrade(seatGrade, concertId)
    }

    override fun findByConcertId(concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGradeRepository.findByConcertId(concertId)
    }

    override fun findById(id: Long): ConcertSeatGrade? {
        return concertSeatGradeRepository.findById(id)
    }
}