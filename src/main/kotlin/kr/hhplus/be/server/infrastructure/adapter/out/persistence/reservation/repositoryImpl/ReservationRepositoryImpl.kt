package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.repositoryImpl

import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa.ReservationJpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class ReservationRepositoryImpl(
    private val reservationRepository: ReservationJpaRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        return PersistenceMapper.toReservationEntity(reservation)
            .let { reservationRepository.save(it) }
            .let { PersistenceMapper.toReservationDomain(it) }
    }
}