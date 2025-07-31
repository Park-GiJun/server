package kr.hhplus.be.server.infrastructure.adapter.out.event

import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueActivationEvent
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueuePositionUpdateEvent
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.service.QueueWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class QueueWebSocketEventPublisher(
    private val queueWebSocketService: QueueWebSocketService
) {
    private val log = LoggerFactory.getLogger(QueueWebSocketEventPublisher::class.java)

    fun publishPositionUpdate(event: QueuePositionUpdateEvent) {
        try {
            log.debug("Publishing position update event for token: ${event.tokenId}")
            queueWebSocketService.broadcastPositionUpdate(event)
        } catch (e: Exception) {
            log.error("Error publishing position update event", e)
        }
    }

    fun publishActivation(event: QueueActivationEvent) {
        try {
            log.info("Publishing activation event for token: ${event.tokenId}")
            queueWebSocketService.notifyActivation(event)
        } catch (e: Exception) {
            log.error("Error publishing activation event", e)
        }
    }

    fun publishExpiration(tokenId: String, message: String = "대기열이 만료되었습니다.") {
        try {
            log.info("Publishing expiration event for token: $tokenId")
            queueWebSocketService.notifyExpiration(tokenId, message)
        } catch (e: Exception) {
            log.error("Error publishing expiration event", e)
        }
    }
}