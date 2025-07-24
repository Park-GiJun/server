import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.TempReservation
import java.time.LocalDateTime

object ReservationMapper {

    fun toTempReservationResult(domain: TempReservation): TempReservationResult {
        return TempReservationResult(
            tempReservationId = domain.tempReservationId,
            userId = domain.userId,
            concertSeatId = domain.concertSeatId,
            expiredAt = domain.expiredAt,
            status = domain.status
        )
    }

    fun toConfirmTempReservationResult(reservation: Reservation): ConfirmTempReservationResult {
        return ConfirmTempReservationResult(
            reservationId = reservation.reservationId,
            userId = reservation.userId,
            concertDateId = reservation.concertDateId,
            seatId = reservation.seatId,
            reservationStatus = reservation.reservationStatus,
            paymentAmount = reservation.paymentAmount,
            reservationAt = LocalDateTime.now()
        )
    }

    fun toCancelReservationResult(domain: TempReservation): CancelReservationResult {
        return CancelReservationResult(
            tempReservationId = domain.tempReservationId,
            userId = domain.userId,
            concertSeatId = domain.concertSeatId,
            expiredAt = domain.expiredAt,
            status = domain.status
        )
    }
}