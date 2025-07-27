package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation

import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.mock.MockTempReservationRepository
import org.springframework.stereotype.Repository

@Repository
class TempReservationRepositoryImpl(
    private val mockTempReservationRepository: TempReservationRepository
) : TempReservationRepository {

    override fun save(tempReservation: TempReservation): TempReservation {
        return mockTempReservationRepository.save(tempReservation)
    }

    override fun findByTempReservationId(tempReservationId: Long): TempReservation? {
        return mockTempReservationRepository.findByTempReservationId(tempReservationId)
    }

    override fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservation? {
        return mockTempReservationRepository.findByUserIdAndConcertSeatId(userId, concertSeatId)
    }
}