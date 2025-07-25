package kr.hhplus.be.server.repository.mock

import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class MockTempReservationRepository {
    private val log = LoggerFactory.getLogger(MockTempReservationRepository::class.java)
    private val tempReservations = ConcurrentHashMap<Long, TempReservation>()
    private val idGenerator = AtomicLong(1)

    fun save(tempReservation: TempReservation): TempReservation {
        val savedReservation = if (tempReservation.tempReservationId == 0L) {
            val newId = idGenerator.getAndIncrement()
            TempReservation(
                tempReservationId = newId,
                userId = tempReservation.userId,
                concertSeatId = tempReservation.concertSeatId,
                expiredAt = tempReservation.expiredAt,
                status = tempReservation.status
            )
        } else {
            tempReservation
        }

        tempReservations[savedReservation.tempReservationId] = savedReservation
        log.info("Saving temp reservation for ID: ${savedReservation.tempReservationId}, User: ${savedReservation.userId}")
        return savedReservation
    }

    fun findByTempReservationId(tempReservationId: Long): TempReservation? {
        return tempReservations[tempReservationId]
    }

    fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservation? {
        return tempReservations.values.find {
            it.userId == userId &&
                    it.concertSeatId == concertSeatId &&
                    it.status == TempReservationStatus.RESERVED
        }
    }
}