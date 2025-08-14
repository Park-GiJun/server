package kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.mapper

import kr.hhplus.be.server.application.dto.reservation.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.CancelReservationResult
import kr.hhplus.be.server.application.dto.reservation.ConfirmTempReservationResult
import kr.hhplus.be.server.application.dto.reservation.TempReservationResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationCancelRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationConfirmRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.TempReservationRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.TempReservationResponse

object ReservationWebMapper {

    fun toTempReservationCommand(request: TempReservationRequest, tokenId: String): TempReservationCommand {
        return TempReservationCommand(
            tokenId = tokenId,
            userId = request.userId,
            concertSeatId = request.concertSeatId
        )
    }

    fun toConfirmTempReservationCommand(request: ReservationConfirmRequest, tokenId: String): ConfirmTempReservationCommand {
        return ConfirmTempReservationCommand(
            tokenId = tokenId,
            tempReservationId = request.tempReservationId,
            paymentAmount = request.paymentAmount
        )
    }

    fun toCancelReservationCommand(request: ReservationCancelRequest, tokenId: String): CancelReservationCommand {
        return CancelReservationCommand(
            tempReservationId = request.tempReservationId
        )
    }

    fun toTempReservationResponse(result: TempReservationResult): TempReservationResponse {
        return TempReservationResponse(
            tempReservationId = result.tempReservationId,
            userId = result.userId,
            concertSeatId = result.concertSeatId,
            expiredAt = result.expiredAt,
            status = result.status
        )
    }

    fun toReservationResponse(result: ConfirmTempReservationResult): ReservationResponse {
        return ReservationResponse(
            reservationId = result.reservationId,
            userId = result.userId,
            concertDateId = result.concertDateId,
            seatId = result.seatId,
            reservationStatus = result.reservationStatus,
            paymentAmount = result.paymentAmount,
            reservationAt = result.reservationAt
        )
    }

    fun toTempReservationResponseFromCancel(result: CancelReservationResult): TempReservationResponse {
        return TempReservationResponse(
            tempReservationId = result.tempReservationId,
            userId = result.userId,
            concertSeatId = result.concertSeatId,
            expiredAt = result.expiredAt,
            status = result.status
        )
    }
}