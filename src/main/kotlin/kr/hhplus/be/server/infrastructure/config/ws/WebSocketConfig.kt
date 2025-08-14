package kr.hhplus.be.server.infrastructure.config.ws

import kr.hhplus.be.server.infrastructure.adapter.`in`.ws.WebSocketQueueHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketQueueHandler: WebSocketQueueHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketQueueHandler, "/ws/queue")
            .setAllowedOrigins("*")
    }
}