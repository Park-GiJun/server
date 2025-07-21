package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.mock

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertDate
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

@Repository
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

@Repository
class MockConcertSeatRepository {
    private val log = LoggerFactory.getLogger(MockConcertSeatRepository::class.java)
    private val seats = ConcurrentHashMap<Long, ConcertSeat>()
    private val idGenerator = AtomicLong(1)

    fun save(concertSeat: ConcertSeat): ConcertSeat {
        val savedSeat = if (concertSeat.concertSeatId == 0L) {
            val newId = idGenerator.getAndIncrement()
            val newSeat = ConcertSeat(
                concertSeatId = newId,
                concertDateId = concertSeat.concertDateId,
                seatNumber = concertSeat.seatNumber,
                seatGrade = concertSeat.seatGrade,
                seatStatus = concertSeat.seatStatus
            )
            seats[newId] = newSeat
            log.info("Inserted new ConcertSeat ID: $newId, SeatNumber: ${newSeat.seatNumber}, Grade: ${newSeat.seatGrade}, Status: ${newSeat.seatStatus}")
            newSeat
        } else {
            seats[concertSeat.concertSeatId] = concertSeat
            log.info("Updated ConcertSeat ID: ${concertSeat.concertSeatId}, SeatNumber: ${concertSeat.seatNumber}, Grade: ${concertSeat.seatGrade}, Status: ${concertSeat.seatStatus}")
            concertSeat
        }

        return savedSeat
    }

    fun findConcertSeats(concertDateId: Long): List<ConcertSeat>? {
        val result = seats.values.filter { it.concertDateId == concertDateId }
            .sortedBy { it.seatNumber.toIntOrNull() ?: Int.MAX_VALUE }
        return result.ifEmpty { null }
    }

    fun findByConcertSeatId(concertSeatId: Long): ConcertSeat? {
        return seats[concertSeatId]
    }
}