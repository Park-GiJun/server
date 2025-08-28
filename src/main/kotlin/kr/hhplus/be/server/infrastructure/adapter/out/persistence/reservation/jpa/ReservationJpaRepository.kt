package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.ReservationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationJpaRepository : JpaRepository<ReservationJpaEntity, Long>{
    fun findByReservationId(reservationId: Long): ReservationJpaEntity?
}