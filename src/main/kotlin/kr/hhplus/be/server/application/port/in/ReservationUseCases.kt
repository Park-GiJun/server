package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult

interface CancelReservationUseCase {
    fun cancelReservation(command: CancelReservationCommand): CancelReservationResult
}

interface ConfirmTempReservationUseCase {
    fun confirmTempReservation(command: ConfirmTempReservationCommand) : ConfirmTempReservationResult
}

interface TempReservationUseCase{
    fun tempReservation(command: TempReservationCommand): TempReservationResult
}