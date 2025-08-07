package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult

interface CancelReservationUseCase {
    fun cancelReservation(command: CancelReservationCommand): CancelReservationResult
}