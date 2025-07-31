package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.handler

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.port.`in`.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.GetQueueStatusUseCase
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueWebSocketConnectRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.service.QueueWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI

@Component
class QueueWebSocketHandler(
    private val queueWebSocketService: QueueWebSocketService,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(QueueWebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val connectionInfo = getConnectionInfoFromSession(session)
            connectionInfo?.let { info ->
                queueWebSocketService.addSession(info.tokenId, info.concertId, session)

                sendInitialQueueStatus(info.tokenId)

                log.info("WebSocket connection established for token: ${info.tokenId}")
            } ?: run {
                log.warn("Invalid connection parameters, closing session")
                session.close(CloseStatus.BAD_DATA)
            }
        } catch (e: Exception) {
            log.error("Error establishing WebSocket connection", e)
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val payload = message.payload
            log.debug("Received WebSocket message: $payload")
        } catch (e: Exception) {
            log.error("Error handling WebSocket message", e)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        try {
            val connectionInfo = getConnectionInfoFromSession(session)
            connectionInfo?.let { info ->
                queueWebSocketService.removeSession(info.tokenId)

                expireTokenUseCase.expireToken(ExpireTokenCommand(info.tokenId))

                log.info("WebSocket disconnected - Token expired: ${info.tokenId}, status: $status")
            }
        } catch (e: Exception) {
            log.error("Error closing WebSocket connection", e)
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.error("WebSocket transport error - Connection will be terminated", exception)

        try {
            val connectionInfo = getConnectionInfoFromSession(session)
            connectionInfo?.let { info ->
                queueWebSocketService.removeSession(info.tokenId)
                expireTokenUseCase.expireToken(ExpireTokenCommand(info.tokenId))
                log.info("Token expired due to transport error: ${info.tokenId}")
            }
        } catch (e: Exception) {
            log.error("Error handling transport error", e)
        }
    }

    private fun getConnectionInfoFromSession(session: WebSocketSession): QueueWebSocketConnectRequest? {
        return try {
            val tokenId = session.attributes["tokenId"] as? String ?: return null
            val userId = session.attributes["userId"] as? String ?: return null
            val concertId = session.attributes["concertId"] as? Long ?: return null

            QueueWebSocketConnectRequest(tokenId, userId, concertId)
        } catch (e: Exception) {
            log.error("Error getting connection info from session", e)
            null
        }
    }

    private fun parseConnectionInfo(session: WebSocketSession): QueueWebSocketConnectRequest? {
        return try {
            val uri = session.uri ?: return null
            val query = parseQueryString(uri)

            val tokenId = query["tokenId"] ?: return null
            val userId = query["userId"] ?: return null
            val concertId = query["concertId"]?.toLongOrNull() ?: return null

            QueueWebSocketConnectRequest(tokenId, userId, concertId)
        } catch (e: Exception) {
            log.error("Error parsing connection info", e)
            null
        }
    }

    private fun parseQueryString(uri: URI): Map<String, String> {
        val query = uri.query ?: return emptyMap()
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }

    private fun sendInitialQueueStatus(tokenId: String) {
        try {
            val queueStatus = getQueueStatusUseCase.getQueueStatus(
                GetQueueStatusQuery(tokenId)
            )

            val message = when (queueStatus.status) {
                QueueTokenStatus.WAITING -> "대기 중입니다. 잠시만 기다려주세요."
                QueueTokenStatus.ACTIVE -> "예약 페이지에 진입할 수 있습니다!"
                QueueTokenStatus.EXPIRED -> "대기열이 만료되었습니다."
                else -> "대기열 상태를 확인 중입니다."
            }

            queueWebSocketService.sendQueueUpdate(
                tokenId = tokenId,
                position = queueStatus.position,
                status = queueStatus.status,
                message = message
            )

        } catch (e: Exception) {
            log.error("Error sending initial queue status for token: $tokenId", e)
            queueWebSocketService.notifyExpiration(tokenId, "대기열 상태를 가져올 수 없습니다.")
        }
    }
}