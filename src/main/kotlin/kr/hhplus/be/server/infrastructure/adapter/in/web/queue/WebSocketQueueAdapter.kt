package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.infrastructure.adapter.out.ws.queue.WebSocketQueueEventAdapter
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ws/queue")
@Tag(name = "대기열 WebSocket", description = "대기열 실시간 알림 (WebSocket)")
class WebSocketQueueAdapter(
    private val webSocketEventAdapter: WebSocketQueueEventAdapter
) {

    private val log = LoggerFactory.getLogger(WebSocketQueueAdapter::class.java)

    @GetMapping("/stats")
    @Operation(summary = "WebSocket 연결 통계", description = "현재 활성 WebSocket 연결 수를 조회합니다")
    fun getWebSocketStats(): Map<String, Any> {
        val activeConnections = webSocketEventAdapter.getActiveConnectionsCount()

        log.info("WebSocket 통계 조회: 활성 연결 수=$activeConnections")

        return mapOf(
            "activeConnections" to activeConnections,
            "connectionType" to "WebSocket",
            "timestamp" to System.currentTimeMillis()
        )
    }
}