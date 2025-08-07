package kr.hhplus.be.server.infrastructure.adapter.out.event.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessage
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessagePort
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketSessionPort
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WebSocketQueueEventAdapter(
    private val webSocketSessionPort: QueueWebSocketSessionPort,
    private val webSocketMessagePort: QueueWebSocketMessagePort
) : QueueEventPort {

    private val log = LoggerFactory.getLogger(WebSocketQueueEventAdapter::class.java)

    override fun publishTokenActivated(tokenId: String, userId: String, concertId: Long) {
        val session = webSocketSessionPort.getSession(tokenId)
        if (session == null) {
            log.warn("세션을 찾을 수 없음: tokenId=$tokenId")
            return
        }

        val message = QueueWebSocketMessage(
            tokenId = tokenId,
            userId = userId,
            concertId = concertId,
            status = QueueTokenStatus.ACTIVE,
            position = 0,
            message = "예약 페이지에 진입할 수 있습니다."
        )

        val success = webSocketMessagePort.sendMessage(tokenId, message)
        if (success) {
            log.info("토큰 활성화 이벤트 발행 성공: tokenId=$tokenId")
        } else {
            log.error("토큰 활성화 이벤트 발행 실패: tokenId=$tokenId")
        }
    }

    override fun publishTokenExpired(tokenId: String) {
        val session = webSocketSessionPort.getSession(tokenId)
        if (session == null) {
            log.debug("만료된 토큰의 세션 없음: tokenId=$tokenId")
            return
        }

        val message = QueueWebSocketMessage(
            tokenId = tokenId,
            userId = session.userId,
            concertId = session.concertId,
            status = QueueTokenStatus.EXPIRED,
            position = -1,
            message = "대기열이 만료되었습니다."
        )

        webSocketMessagePort.sendMessage(tokenId, message)
        log.info("토큰 만료 이벤트 발행: tokenId=$tokenId")
    }

    override fun publishPositionUpdated(tokenId: String, newPosition: Int, concertId: Long) {
        val session = webSocketSessionPort.getSession(tokenId)
        if (session == null) {
            log.debug("포지션 업데이트할 세션 없음: tokenId=$tokenId")
            return
        }

        val message = QueueWebSocketMessage(
            tokenId = tokenId,
            userId = session.userId,
            concertId = concertId,
            status = QueueTokenStatus.WAITING,
            position = newPosition,
            message = "대기 순서가 업데이트되었습니다."
        )

        webSocketMessagePort.sendMessage(tokenId, message)
        log.debug("포지션 업데이트 이벤트 발행: tokenId=$tokenId, position=$newPosition")
    }
}

