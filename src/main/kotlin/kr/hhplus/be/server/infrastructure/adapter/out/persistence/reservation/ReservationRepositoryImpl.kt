package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation

import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.mock.MockReservationRepository
import org.springframework.stereotype.Repository

@Repository
class ReservationRepositoryImpl(
    private val mockReservationRepository: MockReservationRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        return mockReservationRepository.save(reservation)
    }
}