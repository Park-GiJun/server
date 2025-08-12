package kr.hhplus.be.server.infrastructure.adapter.`in`.ws

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.infrastructure.adapter.out.event.websocket.queue.WebSocketQueueEventAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.net.URLDecoder
import kotlin.collections.get

@Component
class WebSocketQueueHandler(
    private val validateTokenUseCase: ValidateQueueTokenUseCase,
    private val webSocketEventAdapter: WebSocketQueueEventAdapter,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(WebSocketQueueHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("WebSocket 연결 시도: sessionId=${session.id}")

        try {
            val params = parseQueryParams(session.uri)
            val tokenId = params["tokenId"] ?: throw IllegalArgumentException("tokenId 파라미터가 필요합니다")
            val userId = params["userId"] ?: throw IllegalArgumentException("userId 파라미터가 필요합니다")
            val concertId = params["concertId"]?.toLongOrNull() ?: throw IllegalArgumentException("concertId 파라미터가 필요합니다")

            log.info("WebSocket 연결 요청: userId=$userId, tokenId=$tokenId, concertId=$concertId")

            val command = ValidateQueueTokenCommand(tokenId, concertId)
            try {
                validateTokenUseCase.validateActiveTokenForConcert(command)
                log.info("ACTIVE 토큰으로 WebSocket 연결: tokenId=$tokenId")
            } catch (e: Exception) {
                log.info("WAITING 토큰으로 WebSocket 연결: tokenId=$tokenId")
            }

            webSocketEventAdapter.registerConnection(userId, tokenId, session)

            val connectionMessage = mapOf(
                "type" to "connected",
                "message" to "WebSocket 연결이 성공했습니다",
                "tokenId" to tokenId,
                "userId" to userId
            )
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(connectionMessage)))

            webSocketEventAdapter.publishQueueEntered(
                tokenId = tokenId,
                userId = userId,
                concertId = concertId,
                position = 0L,
                estimatedWaitTime = 30
            )

            log.info("WebSocket 연결 완료: userId=$userId, 활성 연결 수=${webSocketEventAdapter.getActiveConnectionsCount()}")

        } catch (e: Exception) {
            log.error("WebSocket 연결 실패: sessionId=${session.id}", e)
            val errorMessage = mapOf(
                "type" to "error",
                "message" to e.message
            )
            try {
                session.sendMessage(TextMessage(objectMapper.writeValueAsString(errorMessage)))
                session.close(CloseStatus.BAD_DATA)
            } catch (sendError: Exception) {
                log.error("에러 메시지 전송 실패: sessionId=${session.id}", sendError)
            }
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.info("WebSocket 메시지 수신: sessionId=${session.id}, message=${message.payload}")

        try {
            val messageMap = objectMapper.readValue(message.payload, Map::class.java)

            when (messageMap["type"]) {
                "ping" -> {
                    val pongMessage = mapOf(
                        "type" to "pong",
                        "timestamp" to System.currentTimeMillis()
                    )
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(pongMessage)))
                }
                "status_request" -> {
                    val statusMessage = mapOf(
                        "type" to "status_response",
                        "activeConnections" to webSocketEventAdapter.getActiveConnectionsCount(),
                        "sessionId" to session.id
                    )
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(statusMessage)))
                }
            }
        } catch (e: Exception) {
            log.error("WebSocket 메시지 처리 오류: sessionId=${session.id}", e)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("WebSocket 연결 종료: sessionId=${session.id}, status=$status")

        // 연결 정리
        webSocketEventAdapter.removeConnection(session)

        log.info("WebSocket 연결 정리 완료: sessionId=${session.id}")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.error("WebSocket 전송 오류: sessionId=${session.id}", exception)
        webSocketEventAdapter.removeConnection(session)
    }

    /**
     * URL 쿼리 파라미터 파싱
     */
    private fun parseQueryParams(uri: URI?): Map<String, String> {
        if (uri?.query == null) return emptyMap()

        return uri.query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=")
                if (parts.size == 2) {
                    parts[0] to URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }
            .toMap()
    }
}