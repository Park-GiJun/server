package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import kr.hhplus.be.server.dto.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse
import kr.hhplus.be.server.interfaces.facade.QueueFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/queue")
class QueueWebAdapter(
    private val queueFacade: QueueFacade
) {
    @PostMapping("/token/{concertId}")
    fun generateToken(
        @PathVariable concertId: Long,
        @RequestBody request: GenerateTokenRequest
    ): ResponseEntity<ApiResponse<GenerateTokenResponse>> {

        val tokenId = queueFacade.generateToken(
            userId = request.userId,
            concertId = concertId
        )

        val response = GenerateTokenResponse(
            tokenId = tokenId,
            message = "Queue token generated successfully"
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status/{tokenId}")
    fun getQueueStatus(
        @PathVariable tokenId: String
    ): ResponseEntity<ApiResponse<QueueStatusResponse>> {

        val result = queueFacade.getQueueStatus(tokenId)

        val response = QueueStatusResponse(
            tokenId = result.tokenId,
            userId = result.userId,
            concertId = result.concertId,
            status = result.status,
            position = result.position,
            estimatedWaitTime = result.estimatedWaitTime
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/activate/{concertId}")
    fun activateTokens(
        @PathVariable concertId: Long,
        @RequestParam(defaultValue = "10") count: Int
    ): ResponseEntity<ApiResponse<String>> {

        val activatedTokens = queueFacade.activateNextTokens(concertId, count)

        return ResponseEntity.ok(
            ApiResponse.success("Activated ${activatedTokens.size} tokens")
        )
    }

    @DeleteMapping("/token/{tokenId}")
    fun expireToken(
        @PathVariable tokenId: String
    ): ResponseEntity<ApiResponse<String>> {

        queueFacade.expireToken(tokenId)

        return ResponseEntity.ok(
            ApiResponse.success("Token expired successfully")
        )
    }
}