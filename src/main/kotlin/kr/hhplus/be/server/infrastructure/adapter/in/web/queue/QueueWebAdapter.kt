package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper.QueueWebMapper
import kr.hhplus.be.server.interfaces.facade.QueueFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/queue")
@Tag(name = "대기열", description = "대기열 토큰 관리 API")
class QueueWebAdapter(
    private val queueFacade: QueueFacade
) {

    @PostMapping("/token/{concertId}")
    @Operation(summary = "대기열 토큰 발급")
    fun generateToken(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @RequestBody request: GenerateTokenRequest
    ): ResponseEntity<ApiResponse<GenerateTokenResponse>> {

        val command = QueueWebMapper.toGenerateTokenCommand(request, concertId)
        val tokenId = queueFacade.generateToken(command)
        val response = QueueWebMapper.toGenerateTokenResponse(tokenId)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status/{tokenId}")
    @Operation(summary = "대기열 상태 조회")
    fun getQueueStatus(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable tokenId: String
    ): ResponseEntity<ApiResponse<QueueStatusResponse>> {

        val query = QueueWebMapper.toGetQueueStatusQuery(tokenId)
        val result = queueFacade.getQueueStatus(query)
        val response = QueueWebMapper.toStatusResponse(result)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/activate/{concertId}")
    @Operation(summary = "대기열 토큰 활성화")
    fun activateTokens(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @Parameter(description = "활성화할 토큰 개수", example = "10")
        @RequestParam(defaultValue = "10") count: Int
    ): ResponseEntity<ApiResponse<String>> {

        val result = queueFacade.activateNextTokens(concertId, count)
        return ResponseEntity.ok(ApiResponse.success("Activated ${result.activatedCount} tokens"))
    }

    @DeleteMapping("/token/{tokenId}")
    @Operation(summary = "대기열 토큰 취소")
    fun expireToken(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable tokenId: String
    ): ResponseEntity<ApiResponse<String>> {

        queueFacade.expireToken(tokenId)
        return ResponseEntity.ok(ApiResponse.success("Token expired successfully"))
    }
}