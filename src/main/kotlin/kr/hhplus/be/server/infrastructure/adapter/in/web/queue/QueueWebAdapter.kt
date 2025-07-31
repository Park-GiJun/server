package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper.QueueWebMapper
import kr.hhplus.be.server.application.port.`in`.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/queue")
@Tag(name = "대기열", description = "대기열 토큰 관리 API")
class QueueWebAdapter(
    private val generateTokenUseCase: GenerateTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val activateTokensUseCase: ActivateTokensUseCase,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val validateTokenUseCase: ValidateTokenUseCase
) {

    @PostMapping("/token/{concertId}")
    @Operation(summary = "대기열 토큰 발급")
    fun generateToken(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @RequestBody request: GenerateTokenRequest
    ): ResponseEntity<ApiResponse<GenerateTokenResponse>> {

        val command = QueueWebMapper.toGenerateTokenCommand(request, concertId)
        val tokenId = generateTokenUseCase.generateToken(command)
        val response = QueueWebMapper.toGenerateTokenResponse(tokenId)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status")
    @Operation(summary = "대기열 상태 조회")
    fun getQueueStatus(
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<QueueStatusResponse>> {

        validateTokenUseCase.validateActiveTokenForConcert(
            ValidateTokenCommand(tokenId)
        )

        val query = QueueWebMapper.toGetQueueStatusQuery(tokenId)
        val result = getQueueStatusUseCase.getQueueStatus(query)
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

        val command = ActivateTokensCommand(concertId, count)
        val result = activateTokensUseCase.activateTokens(command)
        return ResponseEntity.ok(ApiResponse.success("Activated ${result.activatedCount} tokens"))
    }

    @DeleteMapping("/token")
    @Operation(summary = "대기열 토큰 취소")
    fun expireToken(
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<String>> {

        validateTokenUseCase.validateActiveTokenForConcert(
            ValidateTokenCommand(tokenId)
        )

        val command = ExpireTokenCommand(tokenId)
        expireTokenUseCase.expireToken(command)
        return ResponseEntity.ok(ApiResponse.success("Token expired successfully"))
    }
}