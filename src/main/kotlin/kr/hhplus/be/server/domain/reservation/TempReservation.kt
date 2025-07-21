package kr.hhplus.be.server.domain.reservation

import jakarta.persistence.*
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseJpaEntity
import java.time.LocalDateTime

@Entity
@Table(name = "temp_reservation")
class TempReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_reservation_id")
    var tempReservationId: Long = 0,

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "concert_seat_id")
    val concertSeatId: Long,

    @Column(name = "expired_at")
    val expiredAt: LocalDateTime,

    @Column(name = "temp_reservation_status")
    @Enumerated(EnumType.STRING)
    val status: TempReservationStatus = TempReservationStatus.RESERVED
) : BaseJpaEntity() {

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiredAt)
    fun isReserved(): Boolean = status == TempReservationStatus.RESERVED
    fun isConfirmed(): Boolean = status == TempReservationStatus.CONFIRMED

    fun confirm(): TempReservation {
        return TempReservation(
            tempReservationId = this.tempReservationId,
            userId = this.userId,
            concertSeatId = this.concertSeatId,
            expiredAt = this.expiredAt,
            status = TempReservationStatus.CONFIRMED
        )
    }

    fun expire(): TempReservation {
        return TempReservation(
            tempReservationId = this.tempReservationId,
            userId = this.userId,
            concertSeatId = this.concertSeatId,
            expiredAt = this.expiredAt,
            status = TempReservationStatus.EXPIRED
        )
    }
}