package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.command.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.ConfirmTempReservationResult

interface ConfirmTempReservationUseCase {
    fun confirmTempReservation(command: ConfirmTempReservationCommand) : ConfirmTempReservationResult
}