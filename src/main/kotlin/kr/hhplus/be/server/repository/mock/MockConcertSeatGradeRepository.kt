package kr.hhplus.be.server.repository.mock

import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockConcertSeatGradeRepository {

    private val log = LoggerFactory.getLogger(MockConcertSeatGradeRepository::class.java)
    private val concertSeatGrades = ConcurrentHashMap<Long, ConcertSeatGrade>()

    fun save(concertSeatGrade: ConcertSeatGrade): ConcertSeatGrade {
        concertSeatGrades[concertSeatGrade.concertSeatGradeId] = concertSeatGrade
        log.info("Saved Concert Seat Grade ${concertSeatGrade.concertSeatGradeId}")
        return concertSeatGrade
    }

    fun findBySeatGrade(seatGrade: String, concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGrades.values.filter {
            it.seatGrade == seatGrade && it.concertId == concertId
        }
    }

    fun findByConcertId(concertId: Long): List<ConcertSeatGrade> {
        return concertSeatGrades.values.filter { it.concertId == concertId }
    }

    fun findById(id: Long): ConcertSeatGrade? {
        return concertSeatGrades[id]
    }

    fun findAll(): List<ConcertSeatGrade> {
        return concertSeatGrades.values.toList()
    }

    fun deleteById(id: Long): Boolean {
        return concertSeatGrades.remove(id) != null
    }

    fun clear() {
        concertSeatGrades.clear()
    }
}