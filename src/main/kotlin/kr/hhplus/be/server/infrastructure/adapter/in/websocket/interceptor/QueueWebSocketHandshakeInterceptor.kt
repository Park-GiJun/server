package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class QueueWebSocketHandshakeInterceptor : HandshakeInterceptor {
    private val log = LoggerFactory.getLogger(QueueWebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        log.info("WebSocket handshake attempt from: ${request.remoteAddress}")

        val query = request.uri.query
        if (query.isNullOrBlank()) {
            log.warn("WebSocket handshake failed: No query parameters")
            return false
        }

        val params = parseQueryString(query)
        val tokenId = params["tokenId"]
        val userId = params["userId"]
        val concertId = params["concertId"]

        if (tokenId.isNullOrBlank() || userId.isNullOrBlank() || concertId.isNullOrBlank()) {
            log.warn("WebSocket handshake failed: Missing required parameters")
            return false
        }

        attributes["tokenId"] = tokenId
        attributes["userId"] = userId
        attributes["concertId"] = concertId.toLongOrNull() ?: 0L

        log.info("WebSocket handshake approved for tokenId: $tokenId, userId: $userId, concertId: $concertId")
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception)
        } else {
            log.info("WebSocket handshake completed successfully")
        }
    }

    private fun parseQueryString(query: String): Map<String, String> {
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }
}