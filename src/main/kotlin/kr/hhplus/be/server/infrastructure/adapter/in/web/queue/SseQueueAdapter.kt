package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.infrastructure.adapter.out.event.sse.queue.SseQueueEventAdapter
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/sse/queue")
@Tag(name = "대기열 SSE", description = "대기열 실시간 알림 (Server-Sent Events)")
class SseQueueAdapter(
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val sseEventAdapter: SseQueueEventAdapter
) {

    private val log = LoggerFactory.getLogger(SseQueueAdapter::class.java)

    @GetMapping("/sse/connect", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(
        summary = "SSE 대기열 연결",
        description = "대기열 상태를 실시간으로 받기 위한 SSE 연결을 생성합니다"
    )
     fun connectSse(
        @Parameter(description = "대기열 토큰 ID")
        @RequestParam tokenId: String,
        @Parameter(description = "사용자 ID")
        @RequestParam userId: String,
        @Parameter(description = "콘서트 ID")
        @RequestParam concertId: Long
    ): SseEmitter {

        log.info("SSE 연결 요청: userId=$userId, tokenId=$tokenId, concertId=$concertId")

        try {
            val command = ValidateQueueTokenCommand(tokenId, concertId)

            try {
                validateTokenUseCase.validateActiveTokenForConcert(command)
                log.info("ACTIVE 토큰으로 SSE 연결: tokenId=$tokenId")
            } catch (e: Exception) {
                log.info("WAITING 토큰으로 SSE 연결: tokenId=$tokenId")
            }

        } catch (e: Exception) {
            log.error("SSE 연결 실패 - 토큰이 존재하지 않음: userId=$userId", e)
            throw e
        }

        // SSE 연결 생성 (5분 타임아웃)
        val emitter = SseEmitter(300_000L)

        // 연결 등록
        sseEventAdapter.registerConnection(userId, tokenId, emitter)

        try {
            // 초기 연결 확인 메시지
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE 연결이 성공했습니다\",\"tokenId\":\"$tokenId\",\"userId\":\"$userId\"}")
            )

            // 대기열 진입 이벤트 즉시 발송 (테스트용)
            sseEventAdapter.publishQueueEntered(
                tokenId = tokenId,
                userId = userId,
                concertId = concertId,
                position = 2L, // 임시 위치
                estimatedWaitTime = 30 // 임시 대기시간
            )

        } catch (e: Exception) {
            log.error("SSE 초기 메시지 전송 실패: userId=$userId", e)
        }

        log.info("SSE 연결 생성 완료: userId=$userId, 활성 연결 수=${sseEventAdapter.getActiveConnectionsCount()}")
        return emitter
    }

    @GetMapping("/sse/stats")
    @Operation(summary = "SSE 연결 통계", description = "현재 활성 SSE 연결 수를 조회합니다")
    fun getSseStats(): Map<String, Any> {
        return mapOf(
            "activeConnections" to sseEventAdapter.getActiveConnectionsCount(),
            "timestamp" to System.currentTimeMillis()
        )
    }
}