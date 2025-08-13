package kr.hhplus.be.server._disable_mock.concert.mock

import kr.hhplus.be.server.domain.concert.ConcertDate
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class MockConcertDateRepository {
    private val log = Logger.getLogger(MockConcertDateRepository::class.simpleName)
    private val concertDates = ConcurrentHashMap<Long, ConcertDate>()

    fun save(concertDate: ConcertDate) : ConcertDate {
        concertDates[concertDate.concertDateId] = concertDate
        log.info("Saved concertDate: ${concertDate.concertDateId}")
        return concertDate
    }

    fun findConcertDateByConcertId(concertId: Long): List<ConcertDate> {
        return concertDates.values.filter { it.concertId == concertId }
    }

    fun findConcertDateByConcertDateId(concertDateId: Long): ConcertDate? {
        return concertDates.values.find { it.concertDateId == concertDateId }
    }
}