package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.repositoryImpl

import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa.TempReservationJpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class TempReservationRepositoryImpl(
    private val tempReservationJpaRepository: TempReservationJpaRepository
) : TempReservationRepository {

    override fun save(tempReservation: TempReservation): TempReservation {
        return PersistenceMapper.toTempReservationEntity(tempReservation)
            .let { tempReservationJpaRepository.save(it) }
            .let { PersistenceMapper.toTempReservationDomain(it) }
    }

    override fun findByTempReservationId(tempReservationId: Long): TempReservation? {
        return tempReservationJpaRepository.findByTempReservationId(tempReservationId)
            ?.let { PersistenceMapper.toTempReservationDomain(it) }
    }

    override fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservation? {
        return tempReservationJpaRepository.findByUserIdAndConcertSeatId(userId, concertSeatId)
            ?.let { PersistenceMapper.toTempReservationDomain(it) }
    }

    override fun findByConcertSeatID(concertSeatId: Long): TempReservation? {
        return tempReservationJpaRepository.findByTempReservationId(concertSeatId)
        ?.let { PersistenceMapper.toTempReservationDomain(it) }
    }

    override fun findAll(): List<TempReservation> {
        return tempReservationJpaRepository.findAll()
            .map { PersistenceMapper.toTempReservationDomain(it) }
    }
}