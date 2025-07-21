package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockConcertRepository {
    private val log = LoggerFactory.getLogger(MockConcertRepository::class.java)
    private val concerts = ConcurrentHashMap<Long, ConcertJpaEntity>()

    fun save(concert: ConcertJpaEntity) : ConcertJpaEntity{
        concerts[concert.concertId] = concert
        log.info("Saved concert: ${concert.concertId}")
        return concert
    }

    fun findConcertList(): List<ConcertJpaEntity>? {
        log.info("Finding all concerts, count: ${concerts.size}")
        return concerts.values.toList()
    }

    fun findByConcertId(concertId: Long) : ConcertJpaEntity? {
        log.info("Finding concert, concertId: $concertId")
        return concerts.values.find { it.concertId == concertId }
    }
}