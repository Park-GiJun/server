package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.queue.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Component
class QueueWebSocketHandshakeInterceptor : HandshakeInterceptor {

    private val log = LoggerFactory.getLogger(QueueWebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        log.info("WebSocket 핸드셰이크 시도")
        log.info("Request URI: ${request.uri}")
        log.info("Remote Address: ${request.remoteAddress}")

        val query = request.uri.query
        if (query.isNullOrBlank()) {
            log.warn("WebSocket 핸드셰이크 실패: 쿼리 파라미터 없음")
            return false
        }

        log.info("Query string: $query")

        val params = parseQueryString(query)
        log.info("Parsed parameters: $params")

        val tokenId = params["tokenId"]
        val userId = params["userId"]
        val concertId = params["concertId"]

        if (tokenId.isNullOrBlank()) {
            log.warn("WebSocket 핸드셰이크 실패: tokenId 누락")
            return false
        }

        if (userId.isNullOrBlank()) {
            log.warn("WebSocket 핸드셰이크 실패: userId 누락")
            return false
        }

        if (concertId.isNullOrBlank()) {
            log.warn("WebSocket 핸드셰이크 실패: concertId 누락")
            return false
        }

        val concertIdLong = concertId.toLongOrNull()
        if (concertIdLong == null || concertIdLong <= 0) {
            log.warn("WebSocket 핸드셰이크 실패: 유효하지 않은 concertId - $concertId")
            return false
        }

        val decodedTokenId = URLDecoder.decode(tokenId, StandardCharsets.UTF_8)
        val decodedUserId = URLDecoder.decode(userId, StandardCharsets.UTF_8)

        attributes["tokenId"] = decodedTokenId
        attributes["userId"] = decodedUserId
        attributes["concertId"] = concertIdLong

        log.info("WebSocket 핸드셰이크 승인")
        log.info("Decoded tokenId: $decodedTokenId")
        log.info("Decoded userId: $decodedUserId")
        log.info("ConcertId: $concertIdLong")

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        if (exception != null) {
            log.error("WebSocket 핸드셰이크 실패", exception)
        } else {
            log.info("WebSocket 핸드셰이크 완료")
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