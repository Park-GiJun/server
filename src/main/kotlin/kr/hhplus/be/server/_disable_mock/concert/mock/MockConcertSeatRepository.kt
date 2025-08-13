package kr.hhplus.be.server._disable_mock.concert.mock

import kr.hhplus.be.server.domain.concert.ConcertSeat
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

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