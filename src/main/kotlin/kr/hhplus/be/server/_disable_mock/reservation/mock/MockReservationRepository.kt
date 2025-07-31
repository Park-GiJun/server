package kr.hhplus.be.server._disable_mock.reservation.mock

import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server._disable_mock.queue.mock.MockQueueTokenRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class MockReservationRepository {
    private val log = LoggerFactory.getLogger(MockQueueTokenRepository::class.java)
    private val reservations = ConcurrentHashMap<Long, Reservation>()

    fun save(reservation: Reservation): Reservation {
        reservations[reservation.reservationId] = reservation
        log.info("Saved reservation ${reservation.reservationId}")
        return reservation
    }
}