package kr.hhplus.be.server.infrastructure.adapter.out.websocket.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessage
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessagePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class SpringWebSocketMessageAdapter(
    private val objectMapper: ObjectMapper
) : QueueWebSocketMessagePort {

    private val log = LoggerFactory.getLogger(SpringWebSocketMessageAdapter::class.java)

    private val webSocketSessions = ConcurrentHashMap<String, WebSocketSession>()

    fun addWebSocketSession(tokenId: String, session: WebSocketSession) {
        webSocketSessions[tokenId] = session
        log.debug("Spring WebSocket 세션 추가: tokenId=$tokenId")
    }

    fun removeWebSocketSession(tokenId: String) {
        webSocketSessions.remove(tokenId)
        log.debug("Spring WebSocket 세션 제거: tokenId=$tokenId")
    }

    override fun sendMessage(tokenId: String, message: QueueWebSocketMessage): Boolean {
        val session = webSocketSessions[tokenId]

        if (session == null || !session.isOpen) {
            log.warn("유효하지 않은 WebSocket 세션: tokenId=$tokenId")
            return false
        }

        return try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            session.sendMessage(TextMessage(jsonMessage))
            log.debug("메시지 전송 성공: tokenId=$tokenId")
            true
        } catch (e: Exception) {
            log.error("메시지 전송 실패: tokenId=$tokenId", e)
            false
        }
    }

    override fun sendToMultiple(tokenIds: List<String>, message: QueueWebSocketMessage): Int {
        var successCount = 0

        tokenIds.forEach { tokenId ->
            if (sendMessage(tokenId, message.copy(tokenId = tokenId))) {
                successCount++
            }
        }

        log.debug("다중 메시지 전송 완료: 성공=${successCount}/${tokenIds.size}")
        return successCount
    }
}
