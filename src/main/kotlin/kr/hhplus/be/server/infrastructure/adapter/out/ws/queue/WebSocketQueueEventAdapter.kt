package kr.hhplus.be.server.infrastructure.adapter.out.ws.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.event.queue.QueuePositionUpdate
import kr.hhplus.be.server.infrastructure.adapter.out.event.sse.queue.QueueEventMessage
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

    // 사용자별 WebSocket 세션 관리
    private val userConnections = ConcurrentHashMap<String, WebSocketSession>()

    // 토큰별 사용자 매핑
    private val tokenToUserMap = ConcurrentHashMap<String, String>()

    // 세션별 사용자 매핑 (연결 해제시 정리를 위해)
    private val sessionToUserMap = ConcurrentHashMap<String, String>()

    /**
     * WebSocket 연결 등록
     */
    fun registerConnection(userId: String, tokenId: String, session: WebSocketSession) {
        log.info("WebSocket 연결 등록: userId=$userId, tokenId=$tokenId, sessionId=${session.id}")

        // 기존 연결이 있으면 정리
        userConnections[userId]?.let { oldSession ->
            if (oldSession.isOpen) {
                try {
                    oldSession.close()
                } catch (e: Exception) {
                    log.warn("기존 WebSocket 연결 종료 실패: userId=$userId", e)
                }
            }
        }

        userConnections[userId] = session
        tokenToUserMap[tokenId] = userId
        sessionToUserMap[session.id] = userId

        log.info("WebSocket 연결 등록 완료: userId=$userId, 활성 연결 수=${userConnections.size}")
    }

    /**
     * WebSocket 연결 제거
     */
    fun removeConnection(session: WebSocketSession) {
        val userId = sessionToUserMap.remove(session.id)
        if (userId != null) {
            userConnections.remove(userId)

            // 해당 사용자의 토큰 매핑도 제거
            tokenToUserMap.entries.removeIf { it.value == userId }

            log.info("WebSocket 연결 제거 완료: userId=$userId, sessionId=${session.id}")
        }
    }

    /**
     * 활성 연결 수 조회
     */
    fun getActiveConnectionsCount(): Int {
        // 실제로 열려있는 연결만 카운트
        val activeCount = userConnections.values.count { it.isOpen }

        // 닫힌 연결 정리
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
            message = "🎉 예약이 가능합니다! 30분 내에 예약을 완료해주세요."
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
        log.debug("WebSocket 배치 위치 업데이트: concertId=$concertId, 업데이트 수=${updates.size}")

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

    /**
     * 특정 사용자에게 메시지 전송
     */
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

            log.debug("WebSocket 이벤트 전송 성공: userId=$userId, type=${event.type}, position=${event.position}")

        } catch (e: Exception) {
            log.error("WebSocket 이벤트 전송 실패: userId=$userId, type=${event.type}", e)

            userConnections.remove(userId)
            tokenToUserMap.entries.removeIf { it.value == userId }
            sessionToUserMap.entries.removeIf { it.value == userId }
        }
    }

    /**
     * 모든 연결에 브로드캐스트 (관리용)
     */
    fun broadcast(message: QueueEventMessage) {
        log.info("WebSocket 브로드캐스트: type=${message.type}, 대상=${userConnections.size}개 연결")

        userConnections.values.forEach { session ->
            if (session.isOpen) {
                try {
                    val messageJson = objectMapper.writeValueAsString(message)
                    session.sendMessage(TextMessage(messageJson))
                } catch (e: Exception) {
                    log.error("브로드캐스트 전송 실패: sessionId=${session.id}", e)
                }
            }
        }
    }

}