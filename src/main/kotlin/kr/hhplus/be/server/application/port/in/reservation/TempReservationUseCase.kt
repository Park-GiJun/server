package kr.hhplus.be.server.application.port.`in`.reservation

import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult

interface TempReservationUseCase{
    fun tempReservation(command: TempReservationCommand): TempReservationResult
}