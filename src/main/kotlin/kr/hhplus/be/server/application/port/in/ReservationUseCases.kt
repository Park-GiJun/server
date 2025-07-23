package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.reservation.command.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.TempReservationResult

interface CancelReservationUseCase {
    fun cancelReservation(command: CancelReservationCommand): CancelReservationResult
}

interface ConfirmTempReservationUseCase {
    fun confirmTempReservation(command: ConfirmTempReservationCommand) : ConfirmTempReservationResult
}

interface TempReservationUseCase{
    fun tempReservation(command: TempReservationCommand): TempReservationResult
}