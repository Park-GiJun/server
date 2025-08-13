package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.TempReservationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TempReservationJpaRepository : JpaRepository<TempReservationJpaEntity, Long> {
    @Query("SELECT te FROM TempReservationJpaEntity te WHERE te.concertSeatId = :concertSeatId AND te.status = 'RESERVED'")
    fun findByTempReservationIdAndStatus_Reserved(tempReservationId: Long): TempReservationJpaEntity?
    fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservationJpaEntity?

}