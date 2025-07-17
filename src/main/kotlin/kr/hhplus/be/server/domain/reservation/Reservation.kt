package kr.hhplus.be.server.domain.reservation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity
import kr.hhplus.be.server.domain.concert.SeatStatus

@Entity
@Table(name = "reservation")
class Reservation(
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