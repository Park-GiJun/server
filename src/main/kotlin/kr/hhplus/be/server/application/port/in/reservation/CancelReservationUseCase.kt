package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.command.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.CancelReservationResult

interface CancelReservationUseCase {
    fun cancelReservation(command: CancelReservationCommand): CancelReservationResult
}