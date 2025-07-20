package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.*
import kr.hhplus.be.server.dto.common.ApiResponse
import kr.hhplus.be.server.service.ReservationService
import kr.hhplus.be.server.util.JwtQueueTokenUtil
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reservations")
@Tag(name = "예약")
class ReservationController(
    private val reservationService: ReservationService,
    private val jwtQueueTokenUtil: JwtQueueTokenUtil
) {

    @PostMapping("/temp")
    @Operation(summary = "임시 예약 생성")
    fun createTempReservation(
        @RequestBody request: TempReservationRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<TempReservationResponse>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        val tempReservation = reservationService.createTempReservation(tokenRequest, request)

        val response = TempReservationResponse(
            tempReservationId = tempReservation.tempReservationId,
            userId = tempReservation.userId,
            concertSeatId = tempReservation.concertSeatId,
            expiredAt = tempReservation.expiredAt,
            status = tempReservation.status
        )

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.CREATED.value(),
            data = response,
            message = "Temporary reservation created successfully"
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse)
    }

    @PostMapping("/confirm")
    @Operation(summary = "예약 확정 (결제 완료)")
    fun confirmReservation(
        @RequestBody request: ReservationConfirmRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<ReservationResponse>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        val reservation = reservationService.confirmReservation(tokenRequest, request)

        val response = ReservationResponse(
            reservationId = reservation.reservationId,
            userId = reservation.userId,
            concertDateId = reservation.concertDateId,
            seatId = reservation.seatId,
            reservationStatus = reservation.reservationStatus,
            paymentAmount = reservation.paymentAmount,
            reservationAt = java.time.LocalDateTime.now()
        )

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Reservation confirmed successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/cancel")
    @Operation(summary = "임시 예약 취소")
    fun cancelReservation(
        @RequestBody request: ReservationCancelRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<TempReservationResponse>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        val canceledTempReservation = reservationService.cancelReservation(tokenRequest, request)

        val response = TempReservationResponse(
            tempReservationId = canceledTempReservation.tempReservationId,
            userId = canceledTempReservation.userId,
            concertSeatId = canceledTempReservation.concertSeatId,
            expiredAt = canceledTempReservation.expiredAt,
            status = canceledTempReservation.status
        )

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Temporary reservation canceled successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}