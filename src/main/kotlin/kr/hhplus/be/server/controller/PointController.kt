package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.PointChargeRequest
import kr.hhplus.be.server.dto.PointResponse
import kr.hhplus.be.server.dto.PointUseRequest
import kr.hhplus.be.server.service.PointService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kr.hhplus.be.server.dto.common.ApiResponse as CommonApiResponse

@RestController
@RequestMapping("/points")
@Tag(name = "포인트", description = "사용자 포인트 관리 API")
class PointController(
    private val pointService: PointService
) {

    @GetMapping("/{userId}")
    @Operation(
        summary = "사용자 포인트 조회",
    )
    fun getPoint(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String
    ): ResponseEntity<CommonApiResponse<PointResponse>> {
        val user = pointService.getPoint(userId)
        val response = PointResponse(
            userId = user.userId,
            userName = user.userName,
            totalPoint = user.totalPoint,
            availablePoint = user.availablePoint,
            usedPoint = user.usedPoint
        )

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Point retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/{userId}/charge")
    @Operation(
        summary = "포인트 충전",
    )
    fun chargePoint(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String,
        @RequestBody request: PointChargeRequest
    ): ResponseEntity<CommonApiResponse<PointResponse>> {
        val user = pointService.chargePoint(userId, request.amount)
        val response = PointResponse(
            userId = user.userId,
            userName = user.userName,
            totalPoint = user.totalPoint,
            availablePoint = user.availablePoint,
            usedPoint = user.usedPoint
        )

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Point charged successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/{userId}/use")
    @Operation(
        summary = "포인트 사용",
    )
    fun usePoint(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String,
        @RequestBody request: PointUseRequest
    ): ResponseEntity<CommonApiResponse<PointResponse>> {
        val user = pointService.usePoint(userId, request.amount)
        val response = PointResponse(
            userId = user.userId,
            userName = user.userName,
            totalPoint = user.totalPoint,
            availablePoint = user.availablePoint,
            usedPoint = user.usedPoint
        )

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Point used successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}