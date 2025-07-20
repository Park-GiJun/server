package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.QueueTokenRequest
import kr.hhplus.be.server.service.QueueService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kr.hhplus.be.server.dto.common.ApiResponse as CommonApiResponse

@RestController
@RequestMapping("/queue")
@Tag(name = "대기열", description = "대기열 토큰 관리 API")
class QueueController(
    private val queueService: QueueService
) {

    @PostMapping("/token/{concertId}")
    @Operation(
        summary = "대기열 토큰 발급",
    )
    fun issueToken(
        @Parameter(description = "콘서트 ID", example = "1") @PathVariable concertId: Long,
        @RequestBody request: QueueTokenRequest
    ): ResponseEntity<CommonApiResponse<String>> {
        val response = queueService.generateQueueToken(request.userId, concertId)

        val apiResponse = CommonApiResponse(
            success = true, status = HttpStatus.OK.value(), data = response, message = "Queue token issued successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/token/status")
    @Operation(
        summary = "대기열 상태 조회",
    )
    fun getQueueStatus(
        @Parameter(
            description = "Bearer 토큰", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        ) @RequestHeader("Authorization") token: String
    ): ResponseEntity<CommonApiResponse<String>> {
        val jwtToken = token.removePrefix("Bearer ")
        val response = queueService.getQueueStatus(jwtToken)

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Queue status retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}