package kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

@Entity
@Table(
    name = "reservation",
    indexes = [
        Index("RESERVEX0", columnList = "user_id"),
        Index("RESERVEX1", columnList = "reservation_status"),
        Index("RESERVEX2", columnList = "user_id, concert_date_id"),
        Index("RESERVEX3", columnList = "user_id,reservation_status"),
        Index("RESERVEX4", columnList = "concert_date_id"),
        Index("RESERVEX5", columnList = "reservation_at DESC"),
    ]
)
class ReservationJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column("reservation_id")
    val reservationId: Long,

    @Column("user_id")
    val userId: String,

    @Column("concert_date_id")
    val concertDateId: Long,

    @Column("seat_id")
    val seatId: Long,

    @Column("reservation_at")
    val reservationAt: Long,

    @Column("cancel_at")
    val cancelAt: Long,

    @Column("reservation_status")
    @Enumerated(EnumType.STRING)
    val reservationStatus: ReservationStatus,

    @Column("payment_amount")
    val paymentAmount: Int
) : BaseEntity() {

    fun isReservable(): Boolean = reservationStatus == ReservationStatus.CONFIRMED
    fun isCancelable(): Boolean = reservationStatus == ReservationStatus.CANCELLED
    fun isReserved(): Boolean = reservationStatus == ReservationStatus.COMPLETED
    fun isNoShow(): Boolean = reservationStatus == ReservationStatus.NO_SHOW
}