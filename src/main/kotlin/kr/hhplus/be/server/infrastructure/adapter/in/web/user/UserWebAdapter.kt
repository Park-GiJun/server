package kr.hhplus.be.server.infrastructure.adapter.`in`.web.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.port.`in`.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.GetUserUseCase
import kr.hhplus.be.server.application.port.`in`.UseUserPointUseCase
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.dto.UserPointChargeRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.dto.UserPointResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.dto.UserPointUseRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.mapper.UserWebMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
@Tag(name = "사용자", description = "사용자 및 포인트 관리 API")
class UserWebAdapter(
    private val getUserUseCase: GetUserUseCase,
    private val chargeUserPointUseCase: ChargeUserPointUseCase,
    private val useUserPointUseCase: UseUserPointUseCase
) {

    @GetMapping("/{userId}/points")
    @Operation(
        summary = "사용자 포인트 조회",
        description = "특정 사용자의 포인트 정보를 조회합니다."
    )
    fun getUserPoints(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String
    ): ResponseEntity<ApiResponse<UserPointResponse>> {

        val command = GetUserCommand(userId)
        val result = getUserUseCase.getUser(command)
        val response = UserWebMapper.toPointResponse(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "User points retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/{userId}/points/charge")
    @Operation(
        summary = "포인트 충전",
        description = "사용자의 포인트를 충전합니다."
    )
    fun chargeUserPoint(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String,
        @RequestBody request: UserPointChargeRequest
    ): ResponseEntity<ApiResponse<UserPointResponse>> {

        val command = ChargeUserPointCommand(userId, request.amount)
        val result = chargeUserPointUseCase.chargeUserPoint(command)
        val response = UserWebMapper.toPointResponse(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "User points charged successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/{userId}/points/use")
    @Operation(
        summary = "포인트 사용",
        description = "사용자의 포인트를 사용합니다."
    )
    fun useUserPoint(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String,
        @RequestBody request: UserPointUseRequest
    ): ResponseEntity<ApiResponse<UserPointResponse>> {

        val command = UseUserPointCommand(userId, request.amount)
        val result = useUserPointUseCase.useUserPoint(command)
        val response = UserWebMapper.toPointResponse(result)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "User points used successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}