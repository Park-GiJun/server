package kr.hhplus.be.server.infrastructure.config.websocket

import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.queue.QueueWebSocketHandler
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.queue.interceptor.QueueWebSocketHandshakeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val queueWebSocketHandler: QueueWebSocketHandler,
    private val handshakeInterceptor: QueueWebSocketHandshakeInterceptor
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(queueWebSocketHandler, "/ws/queue")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOriginPatterns("*")
            .setAllowedOrigins("*")
    }
}