package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.port.`in`.queue.*
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 대기열 웹 어댑터 (완전 새로 설계)
 * - Redis 기반 대기열 시스템의 REST API
 * - 헥사고날 아키텍처 Inbound Adapter
 */
@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "대기열", description = "Redis 기반 대기열 관리 API")
class QueueWebAdapter(
    private val generateQueueTokenUseCase: GenerateQueueTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val validateQueueTokenUseCase: ValidateQueueTokenUseCase,
    private val expireQueueTokenUseCase: ExpireQueueTokenUseCase
) {

    @PostMapping("/token/{concertId}")
    @Operation(
        summary = "대기열 토큰 발급",
        description = "콘서트 예약을 위한 대기열 토큰을 발급받습니다"
    )
    suspend fun generateToken(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @RequestBody request: GenerateTokenRequest
    ): ResponseEntity<ApiResponse<GenerateTokenResponse>> {

        val command = GenerateQueueTokenCommand(
            userId = request.userId,
            concertId = concertId
        )

        val result = generateQueueTokenUseCase.generateToken(command)

        val response = GenerateTokenResponse(
            tokenId = result.tokenId,
            position = result.position,
            estimatedWaitTime = result.estimatedWaitTime,
            status = result.status.name,
            message = when (result.status) {
                QueueTokenStatus.ACTIVE -> "즉시 예약 가능합니다"
                QueueTokenStatus.WAITING -> "대기열에 진입했습니다. 순서: ${result.position + 1}번째"
                else -> "토큰이 발급되었습니다"
            }
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status")
    @Operation(
        summary = "대기열 상태 조회",
        description = "현재 대기열에서의 위치와 예상 대기 시간을 조회합니다"
    )
    suspend fun getQueueStatus(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<QueueStatusResponse>> {

        val query = GetQueueStatusQuery(tokenId = tokenId)
        val result = getQueueStatusUseCase.getQueueStatus(query)

        val response = QueueStatusResponse(
            tokenId = result.tokenId,
            status = result.status.name,
            position = result.position,
            estimatedWaitTime = result.estimatedWaitTime,
            message = when (result.status) {
                QueueTokenStatus.ACTIVE -> "예약 가능 상태입니다"
                QueueTokenStatus.WAITING -> "대기 중입니다. 순서: ${result.position + 1}번째"
                QueueTokenStatus.EXPIRED -> "토큰이 만료되었습니다"
                QueueTokenStatus.COMPLETED -> "예약이 완료되었습니다"
            }
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/token")
    @Operation(
        summary = "대기열 토큰 만료",
        description = "대기열에서 나가거나 토큰을 만료시킵니다"
    )
    suspend fun expireToken(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<String>> {

        val command = ExpireQueueTokenCommand(
            tokenId = tokenId,
            reason = "사용자가 대기열을 종료했습니다"
        )

        expireQueueTokenUseCase.expireToken(command)

        return ResponseEntity.ok(ApiResponse.success("대기열에서 성공적으로 나갔습니다"))
    }

    @PostMapping("/validate")
    @Operation(
        summary = "토큰 검증",
        description = "예약 진행을 위한 토큰 유효성을 검증합니다"
    )
    suspend fun validateToken(
        @RequestBody request: ValidateTokenRequest
    ): ResponseEntity<ApiResponse<ValidateTokenResponse>> {

        val command = ValidateQueueTokenCommand(
            tokenId = request.tokenId,
            concertId = request.concertId
        )

        val result = validateQueueTokenUseCase.validateToken(command)

        val response = ValidateTokenResponse(
            tokenId = result.tokenId,
            isValid = result.isValid,
            status = result.status.name,
            message = if (result.isValid) "유효한 토큰입니다" else "유효하지 않은 토큰입니다"
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}

// ==================== Request/Response DTOs ====================

data class GenerateTokenRequest(
    val userId: String
)

data class GenerateTokenResponse(
    val tokenId: String,
    val position: Long,
    val estimatedWaitTime: Int,
    val status: String,
    val message: String
)

data class QueueStatusResponse(
    val tokenId: String,
    val status: String,
    val position: Long,
    val estimatedWaitTime: Int,
    val message: String
)

data class ValidateTokenRequest(
    val tokenId: String,
    val concertId: Long
)

data class ValidateTokenResponse(
    val tokenId: String,
    val isValid: Boolean,
    val status: String,
    val message: String
)