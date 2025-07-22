package kr.hhplus.be.server.application.dto.reservation.result

import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import java.time.LocalDateTime

data class TempReservationResult(
    val tempReservationId: Long,
    val userId: Long,
    val concertSeatId: Long,
    val expiredAt: LocalDateTime,
    val status: TempReservationStatus
)
