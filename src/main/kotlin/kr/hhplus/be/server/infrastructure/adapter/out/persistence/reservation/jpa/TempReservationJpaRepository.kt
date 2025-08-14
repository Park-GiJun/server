package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.TempReservationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TempReservationJpaRepository : JpaRepository<TempReservationJpaEntity, Long> {

    @Query("SELECT te FROM TempReservationJpaEntity te WHERE te.tempReservationId = :tempReservationId AND te.status = 'RESERVED'")
    fun findByTempReservationIdAndStatus_Reserved(@Param("tempReservationId") tempReservationId: Long): TempReservationJpaEntity?

    fun findByUserIdAndConcertSeatId(userId: String, concertSeatId: Long): TempReservationJpaEntity?

    @Query("SELECT te FROM TempReservationJpaEntity te WHERE te.concertSeatId = :concertSeatId AND te.status = 'RESERVED'")
    fun findByConcertSeatIdAndStatusReserved(@Param("concertSeatId") concertSeatId: Long): TempReservationJpaEntity?
}