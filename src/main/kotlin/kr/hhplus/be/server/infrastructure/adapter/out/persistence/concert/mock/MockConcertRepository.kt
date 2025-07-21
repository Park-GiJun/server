package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock

import kr.hhplus.be.server.domain.concert.Concert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockConcertRepository {
    private val log = LoggerFactory.getLogger(MockConcertRepository::class.java)
    private val concerts = ConcurrentHashMap<Long, Concert>()

    fun save(concert: Concert) : Concert {
        concerts[concert.concertId] = concert
        log.info("Saved concert: ${concert.concertId}")
        return concert
    }

    fun findConcertList(): List<Concert>? {
        log.info("Finding all concerts, count: ${concerts.size}")
        return concerts.values.toList()
    }

    fun findByConcertId(concertId: Long) : Concert? {
        log.info("Finding concert, concertId: $concertId")
        return concerts.values.find { it.concertId == concertId }
    }
}