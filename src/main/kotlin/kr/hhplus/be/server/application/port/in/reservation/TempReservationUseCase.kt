package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.command.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.TempReservationResult

interface TempReservationUseCase{
    fun tempReservation(command: TempReservationCommand): TempReservationResult
}