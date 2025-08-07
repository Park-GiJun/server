package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult

interface ConfirmTempReservationUseCase {
    fun confirmTempReservation(command: ConfirmTempReservationCommand) : ConfirmTempReservationResult
}