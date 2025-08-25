package kr.hhplus.be.server.infrastructure.adapter.out.ws.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.event.queue.QueuePositionUpdate
import kr.hhplus.be.server.infrastructure.adapter.out.event.queue.QueueEventMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketQueueEventAdapter(
    private val objectMapper: ObjectMapper
) : QueueEventPort {

    private val log = LoggerFactory.getLogger(WebSocketQueueEventAdapter::class.java)
    private val userConnections = ConcurrentHashMap<String, WebSocketSession>()
    private val tokenToUserMap = ConcurrentHashMap<String, String>()
    private val sessionToUserMap = ConcurrentHashMap<String, String>()

    fun registerConnection(userId: String, tokenId: String, session: WebSocketSession) {
        userConnections[userId] = session
        tokenToUserMap[tokenId] = userId
        sessionToUserMap[session.id] = userId
    }

    fun removeConnection(session: WebSocketSession) {
        val userId = sessionToUserMap.remove(session.id)
        if (userId != null) {
            userConnections.remove(userId)
            tokenToUserMap.entries.removeIf { it.value == userId }
        }
    }

    fun getActiveConnectionsCount(): Int {
        val activeCount = userConnections.values.count { it.isOpen }
        userConnections.entries.removeIf { !it.value.isOpen }
        return activeCount
    }

    override fun publishQueueEntered(
        tokenId: String,
        userId: String,
        concertId: Long,
        position: Long,
        estimatedWaitTime: Int
    ) {
        val event = QueueEventMessage(
            type = "QUEUE_ENTERED",
            tokenId = tokenId,
            concertId = concertId,
            status = "WAITING",
            position = position.toInt(),
            estimatedWaitTime = estimatedWaitTime,
            message = "대기열 ${position}번째 - 예상 대기시간: ${estimatedWaitTime}분"
        )

        sendToUser(userId, event)
    }

    override suspend fun publishPositionUpdated(
        tokenId: String,
        userId: String,
        concertId: Long,
        newPosition: Long,
        estimatedWaitTime: Int
    ) {
        val event = QueueEventMessage(
            type = "POSITION_UPDATED",
            tokenId = tokenId,
            concertId = concertId,
            status = "WAITING",
            position = newPosition.toInt(),
            estimatedWaitTime = estimatedWaitTime,
            message = "대기 순서 업데이트: ${newPosition}번째 (예상 ${estimatedWaitTime}분)"
        )
        sendToUser(userId, event)
    }

    override fun publishTokenActivated(
        tokenId: String,
        userId: String,
        concertId: Long
    ) {
        val event = QueueEventMessage(
            type = "TOKEN_ACTIVATED",
            tokenId = tokenId,
            concertId = concertId,
            status = "ACTIVE",
            position = 0,
            estimatedWaitTime = 0,
            message = "🎉 예약이 가능합니다!"
        )

        sendToUser(userId, event)
    }

    override suspend fun publishTokenExpired(
        tokenId: String,
        userId: String,
        concertId: Long,
        reason: String
    ) {
        val event = QueueEventMessage(
            type = "TOKEN_EXPIRED",
            tokenId = tokenId,
            concertId = concertId,
            status = "EXPIRED",
            position = 0,
            estimatedWaitTime = 0,
            message = "토큰이 만료되었습니다. 사유: $reason"
        )

        sendToUser(userId, event)
    }

    override suspend fun publishBatchPositionUpdates(
        concertId: Long,
        updates: List<QueuePositionUpdate>
    ) {
        updates.forEach { update ->
            val userId = tokenToUserMap[update.tokenId]
            if (userId != null) {
                publishPositionUpdated(
                    update.tokenId,
                    userId,
                    concertId,
                    update.newPosition,
                    update.estimatedWaitTime
                )
            } else {
                log.warn("배치 업데이트에서 토큰에 해당하는 사용자를 찾을 수 없음: tokenId=${update.tokenId}")
            }
        }
    }

    private fun sendToUser(userId: String, event: QueueEventMessage) {
        val session = userConnections[userId]

        if (session == null) {
            log.debug("WebSocket 연결을 찾을 수 없음: userId=$userId")
            return
        }

        if (!session.isOpen) {
            log.warn("WebSocket 연결이 닫힘: userId=$userId")
            userConnections.remove(userId)
            return
        }

        try {
            val messageJson = objectMapper.writeValueAsString(event)
            session.sendMessage(TextMessage(messageJson))
        } catch (e: Exception) {
            userConnections.remove(userId)
            tokenToUserMap.entries.removeIf { it.value == userId }
            sessionToUserMap.entries.removeIf { it.value == userId }
        }
    }
}