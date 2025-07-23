package kr.hhplus.be.server.application.service.reservation

import kr.hhplus.be.server.application.dto.queue.command.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.ValidateTokenResult
import kr.hhplus.be.server.application.dto.reservation.command.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.result.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.result.TempReservationResult
import kr.hhplus.be.server.application.port.`in`.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.TempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository

class ReservationCommandService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
) : CancelReservationUseCase, TempReservationUseCase, ConfirmTempReservationUseCase, ValidateTokenUseCase {
    override fun cancelReservation(command: CancelReservationCommand): CancelReservationResult {
        TODO("Not yet implemented")
    }

    override fun tempReservation(command: TempReservationCommand): TempReservationResult {
        TODO("Not yet implemented")
    }

    override fun confirmTempReservation(command: ConfirmTempReservationCommand): ConfirmTempReservationResult {
        TODO("Not yet implemented")
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        TODO("Not yet implemented")
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        TODO("Not yet implemented")
    }


}