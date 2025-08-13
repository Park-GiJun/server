package kr.hhplus.be.server._disable_mock.concert.mock

import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
}