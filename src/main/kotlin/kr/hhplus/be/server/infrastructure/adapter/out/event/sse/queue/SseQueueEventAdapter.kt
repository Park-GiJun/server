package kr.hhplus.be.server.infrastructure.adapter.out.event.sse.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.event.queue.QueuePositionUpdate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseQueueEventAdapter(
    private val objectMapper: ObjectMapper
) : QueueEventPort {

    private val log = LoggerFactory.getLogger(SseQueueEventAdapter::class.java)

    // 사용자별 SSE 연결 관리
    private val userConnections = ConcurrentHashMap<String, SseEmitter>()
    // 토큰별 사용자 매핑
    private val tokenToUserMap = ConcurrentHashMap<String, String>()

    /**
     * SSE 연결 등록
     */
    fun registerConnection(userId: String, tokenId: String, emitter: SseEmitter) {
        log.info("SSE 연결 등록: userId=$userId, tokenId=$tokenId")

        userConnections[userId] = emitter
        tokenToUserMap[tokenId] = userId

        // 연결 해제 시 정리
        emitter.onCompletion {
            log.info("SSE 연결 완료: userId=$userId")
            cleanup(userId, tokenId)
        }

        emitter.onTimeout {
            log.warn("SSE 연결 타임아웃: userId=$userId")
            cleanup(userId, tokenId)
        }

        emitter.onError { throwable ->
            log.error("SSE 연결 오류: userId=$userId", throwable)
            cleanup(userId, tokenId)
        }
    }

    /**
     * 연결 정리
     */
    private fun cleanup(userId: String, tokenId: String) {
        userConnections.remove(userId)
        tokenToUserMap.remove(tokenId)
        log.info("SSE 연결 정리 완료: userId=$userId, tokenId=$tokenId")
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
            message = "대기열에 진입했습니다. 순서: ${position}번째"
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
            message = "대기 순서가 업데이트되었습니다. 현재 순서: ${newPosition}번째"
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
            message = "🎉 예약이 가능합니다! 5분 내에 예약을 완료해주세요."
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
            position = -1,
            estimatedWaitTime = 0,
            message = "토큰이 만료되었습니다. 사유: $reason"
        )

        sendToUser(userId, event)
    }

    override suspend fun publishBatchPositionUpdates(
        concertId: Long,
        updates: List<QueuePositionUpdate>
    ) {
        log.info("배치 위치 업데이트: concertId=$concertId, 업데이트 수=${updates.size}")

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
            }
        }
    }

    /**
     * 특정 사용자에게 이벤트 전송
     */
    private fun sendToUser(userId: String, event: QueueEventMessage) {
        val emitter = userConnections[userId]
        if (emitter == null) {
            log.warn("SSE 연결을 찾을 수 없음: userId=$userId")
            return
        }

        try {
            val json = objectMapper.writeValueAsString(event)
            emitter.send(
                SseEmitter.event()
                    .name("queue-update")
                    .data(json)
            )
            log.debug("SSE 이벤트 전송 성공: userId=$userId, type=${event.type}")
        } catch (e: Exception) {
            log.error("SSE 이벤트 전송 실패: userId=$userId", e)
            cleanup(userId, event.tokenId)
        }
    }

    /**
     * 활성 연결 수 조회
     */
    fun getActiveConnectionsCount(): Int = userConnections.size

    /**
     * 특정 콘서트의 연결된 사용자 수 조회 (필요시)
     */
    fun getConnectedUsersForConcert(concertId: Long): List<String> {
        return userConnections.keys.toList()
    }
}