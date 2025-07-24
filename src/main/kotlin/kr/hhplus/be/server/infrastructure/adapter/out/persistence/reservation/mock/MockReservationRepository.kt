package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.mock

import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.mock.MockQueueTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockReservationRepository {
    private val log = LoggerFactory.getLogger(MockQueueTokenRepository::class.java)
    private val reservations = ConcurrentHashMap<Long, Reservation>()

    fun save(reservation: Reservation): Reservation {
        reservations[reservation.reservationId] = reservation
        log.info("Saved reservation ${reservation.reservationId}")
        return reservation
    }
}