package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.port.`in`.queue.GenerateQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper.QueueWebMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/queue")
@Tag(name = "대기열", description = "대기열 토큰 관리 API")
class QueueWebAdapter(
    private val generateQueueTokenUseCase: GenerateQueueTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase
) {

    @PostMapping("/token/{concertId}")
    @Operation(
        summary = "대기열 토큰 발급",
        description = "콘서트 예약을 위한 대기열 토큰을 발급받습니다"
    )
    fun generateToken(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @RequestBody request: GenerateTokenRequest
    ): ResponseEntity<ApiResponse<GenerateTokenResponse>> {

        val command = QueueWebMapper.toGenerateQueueTokenCommand(request.userId, concertId)
        val result = generateQueueTokenUseCase.generateToken(command)
        val response = QueueWebMapper.toResponse(result)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status")
    @Operation(
        summary = "대기열 상태 조회",
        description = "현재 대기열에서의 위치와 예상 대기 시간을 조회합니다"
    )
    fun getQueueStatus(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<QueueStatusResponse>> {

        val query = QueueWebMapper.toGetQueueStatusQuery(tokenId)
        val result = getQueueStatusUseCase.getQueueStatus(query)
        val response = QueueWebMapper.toResponse(result)

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}