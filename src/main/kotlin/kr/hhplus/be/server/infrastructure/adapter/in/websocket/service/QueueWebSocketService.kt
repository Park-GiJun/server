package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueActivationEvent
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueuePositionUpdateEvent
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueWebSocketResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.event.TokenExpiredByDisconnectionEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class QueueWebSocketService(
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher // ✅ 이벤트 퍼블리셔로 변경
) {
    private val log = LoggerFactory.getLogger(QueueWebSocketService::class.java)

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val tokenConcertMap = ConcurrentHashMap<String, Long>()
    private val concertSessions = ConcurrentHashMap<Long, MutableSet<String>>()

    fun addSession(tokenId: String, concertId: Long, session: WebSocketSession) {
        sessions[tokenId] = session
        tokenConcertMap[tokenId] = concertId
        concertSessions.computeIfAbsent(concertId) { mutableSetOf() }.add(tokenId)
    }

    fun removeSession(tokenId: String) {
        sessions.remove(tokenId)?.let { session ->
            tokenConcertMap.remove(tokenId)?.let { concertId ->
                concertSessions[concertId]?.remove(tokenId)
                if (concertSessions[concertId]?.isEmpty() == true) {
                    concertSessions.remove(concertId)
                }
            }
            publishTokenExpiredEvent(tokenId, "WebSocket session removed")
        }
    }

    private fun publishTokenExpiredEvent(tokenId: String, reason: String) {
        try {
            eventPublisher.publishEvent(TokenExpiredByDisconnectionEvent(tokenId, reason))
        } catch (e: Exception) {
            log.error("Failed to publish token expiry event: $tokenId", e)
        }
    }

    fun sendQueueUpdate(tokenId: String, position: Int, status: QueueTokenStatus, message: String? = null) {
        sessions[tokenId]?.let { session ->
            tokenConcertMap[tokenId]?.let { concertId ->
                val response = QueueWebSocketResponse(
                    tokenId = tokenId,
                    userId = "",
                    concertId = concertId,
                    status = status,
                    position = position,
                    message = message
                )
                sendMessage(session, response)
            }
        }
    }

    fun broadcastPositionUpdate(event: QueuePositionUpdateEvent) {
        val concertId = event.concertId
        concertSessions[concertId]?.forEach { tokenId ->
            sessions[tokenId]?.let { session ->
                val response = QueueWebSocketResponse(
                    tokenId = tokenId,
                    userId = event.userId,
                    concertId = concertId,
                    status = event.status,
                    position = event.newPosition,
                    message = "대기열 위치가 업데이트되었습니다."
                )
                sendMessage(session, response)
            }
        }
    }

    fun notifyActivation(event: QueueActivationEvent) {
        sessions[event.tokenId]?.let { session ->
            val response = QueueWebSocketResponse(
                tokenId = event.tokenId,
                userId = event.userId,
                concertId = event.concertId,
                status = QueueTokenStatus.ACTIVE,
                position = 0,
                message = "예약 페이지에 진입할 수 있습니다!"
            )
            sendMessage(session, response)
        }
    }

    fun notifyExpiration(tokenId: String, message: String = "연결이 만료되었습니다.") {
        sessions[tokenId]?.let { session ->
            tokenConcertMap[tokenId]?.let { concertId ->
                val response = QueueWebSocketResponse(
                    tokenId = tokenId,
                    userId = "",
                    concertId = concertId,
                    status = QueueTokenStatus.EXPIRED,
                    position = -1,
                    message = message
                )
                sendMessage(session, response)
                removeSessionOnly(tokenId)
            }
        }
    }

    private fun removeSessionOnly(tokenId: String) {
        sessions.remove(tokenId)?.let {
            tokenConcertMap.remove(tokenId)?.let { concertId ->
                concertSessions[concertId]?.remove(tokenId)
                if (concertSessions[concertId]?.isEmpty() == true) {
                    concertSessions.remove(concertId)
                }
            }
        }
    }

    private fun sendMessage(session: WebSocketSession, response: QueueWebSocketResponse) {
        try {
            if (session.isOpen) {
                val message = TextMessage(objectMapper.writeValueAsString(response))
                session.sendMessage(message)
            } else {
                publishTokenExpiredEvent(response.tokenId, "Session already closed")
            }
        } catch (e: Exception) {
            publishTokenExpiredEvent(response.tokenId, "Message send failed: ${e.message}")
        }
    }
}