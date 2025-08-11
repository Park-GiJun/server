package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.ReservationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ReservationJpaRepository : JpaRepository<ReservationJpaEntity, Long>