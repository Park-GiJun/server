package kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.dto.reservation.command.CancelReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.ConfirmTempReservationCommand
import kr.hhplus.be.server.application.dto.reservation.command.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.CancelReservationUseCase
import kr.hhplus.be.server.application.port.`in`.ConfirmTempReservationUseCase
import kr.hhplus.be.server.application.port.`in`.TempReservationUseCase
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationCancelRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationConfirmRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.ReservationResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.TempReservationRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.dto.TempReservationResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.reservation.mapper.ReservationWebMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reservations")
@Tag(name = "예약", description = "콘서트 좌석 예약 관리 API")
class ReservationWebAdapter(
    private val tempReservationUseCase: TempReservationUseCase,
    private val confirmTempReservationUseCase: ConfirmTempReservationUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase
) {

    @PostMapping("/temp")
    @Operation(
        summary = "임시 예약 생성",
        description = "선택한 좌석에 대해 5분간 유효한 임시 예약을 생성합니다. 활성화된 대기열 토큰이 필요합니다."
    )
    fun createTempReservation(
        @RequestBody request: TempReservationRequest,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<TempReservationResponse>> {

        val command = ReservationWebMapper.toTempReservationCommand(request, tokenId)
        val result = tempReservationUseCase.tempReservation(command)
        val response = ReservationWebMapper.toTempReservationResponse(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.CREATED.value(),
            data = response,
            message = "Temporary reservation created successfully"
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse)
    }

    @PostMapping("/confirm")
    @Operation(
        summary = "예약 확정 (결제 완료)",
        description = "임시 예약을 확정하고 결제를 완료합니다. 대기열 토큰이 완료 상태로 변경됩니다."
    )
    fun confirmReservation(
        @RequestBody request: ReservationConfirmRequest,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<ReservationResponse>> {

        val command = ReservationWebMapper.toConfirmTempReservationCommand(request, tokenId)
        val result = confirmTempReservationUseCase.confirmTempReservation(command)
        val response = ReservationWebMapper.toReservationResponse(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Reservation confirmed successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/cancel")
    @Operation(
        summary = "임시 예약 취소",
        description = "생성된 임시 예약을 취소합니다. 좌석이 다시 예약 가능 상태가 되고 대기열 토큰이 만료됩니다."
    )
    fun cancelReservation(
        @RequestBody request: ReservationCancelRequest,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<TempReservationResponse>> {

        val command = ReservationWebMapper.toCancelReservationCommand(request, tokenId)
        val result = cancelReservationUseCase.cancelReservation(command)
        val response = ReservationWebMapper.toTempReservationResponseFromCancel(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Temporary reservation canceled successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}