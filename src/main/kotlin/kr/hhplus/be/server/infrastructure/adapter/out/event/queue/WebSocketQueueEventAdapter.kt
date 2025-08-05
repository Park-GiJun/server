package kr.hhplus.be.server.infrastructure.adapter.out.event.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory

class WebSocketQueueEventAdapter(
    private val queueWebSocketService: QueueWebSocketService
): QueueEventPort {

    private val log = LoggerFactory.getLogger(WebSocketQueueEventAdapter::class.java)

    override fun publishTokenActivated(tokenId: String, userId: String, concertId: Long, message: String) {
        try {
            queueWebSocketService.sendQueueUpdate(
                tokenId = tokenId,
                position = 0,
                status = QueueTokenStatus.ACTIVE,
                message = message
            )
            log.info("이벤트 발행 : $tokenId")
        } catch (e: Exception) {
            log.info("이벤트 발행 실패 : $tokenId", e)
        }
    }

    override fun publishPositionUpdated(tokenId: String, newPosition: Int, message: String) {
        try {
            queueWebSocketService.sendQueueUpdate(
                tokenId = tokenId,
                position = newPosition,
                status = QueueTokenStatus.WAITING,
                message = message
            )
            log.info("이벤트 발행 : $tokenId, 순번 : $newPosition")
        } catch (e: Exception) {
            log.info("이벤트 발행 실패 : $tokenId", e)
        }
    }
}

