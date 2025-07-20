package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.QueueTokenRequest
import kr.hhplus.be.server.dto.QueueTokenResponse
import kr.hhplus.be.server.dto.QueueStatusResponse
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
        description = "지정된 콘서트에 대한 대기열 토큰을 발급합니다. 이미 활성 토큰이 있으면 기존 토큰을 반환합니다."
    )
    fun issueToken(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @RequestBody request: QueueTokenRequest
    ): ResponseEntity<CommonApiResponse<QueueTokenResponse>> {
        val tokenId = queueService.generateQueueToken(request.userId, concertId)

        val response = QueueTokenResponse(
            tokenId = tokenId,
            message = "Queue token issued successfully"
        )

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Queue token issued successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/status/{tokenId}")
    @Operation(
        summary = "대기열 상태 조회",
        description = "토큰 ID로 현재 대기열 상태를 조회합니다. 대기 순서와 예상 대기시간을 포함합니다."
    )
    fun getQueueStatus(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable tokenId: String
    ): ResponseEntity<CommonApiResponse<QueueStatusResponse>> {
        val response = queueService.getQueueStatus(tokenId)

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Queue status retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @PostMapping("/activate/{concertId}")
    @Operation(
        summary = "대기열 토큰 활성화",
        description = "지정된 콘서트의 대기 중인 토큰들을 순서대로 활성화합니다. (관리자 API)"
    )
    fun activateTokens(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @Parameter(description = "활성화할 토큰 개수", example = "10")
        @RequestParam(defaultValue = "10") count: Int
    ): ResponseEntity<CommonApiResponse<String>> {
        val activatedTokens = queueService.activateNextTokens(concertId, count)

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = "Activated ${activatedTokens.size} tokens",
            message = "Tokens activated successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @DeleteMapping("/token/{tokenId}")
    @Operation(
        summary = "대기열 토큰 취소",
        description = "사용자가 대기열에서 나가고 싶을 때 토큰을 취소합니다."
    )
    fun cancelToken(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable tokenId: String
    ): ResponseEntity<CommonApiResponse<String>> {
        queueService.expireToken(tokenId)

        val apiResponse = CommonApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = "Token cancelled",
            message = "Queue token cancelled successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}