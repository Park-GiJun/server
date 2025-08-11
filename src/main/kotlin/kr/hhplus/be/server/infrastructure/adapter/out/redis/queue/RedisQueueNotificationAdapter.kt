package kr.hhplus.be.server.infrastructure.adapter.out.redis.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.queue.QueueNotificationPort
import kr.hhplus.be.server.application.port.out.queue.QueuePositionUpdate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

/**
 * Redis Pub/Sub 기반 대기열 알림 어댑터
 * - Redis Pub/Sub으로 실시간 알림
 * - WebSocket 구독자들에게 메시지 전달
 * - 배치 알림으로 성능 최적화
 */
@Component
class RedisQueueNotificationAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : QueueNotificationPort {

    private val log = LoggerFactory.getLogger(RedisQueueNotificationAdapter::class.java)

    companion object {
        // Redis 채널 패턴
        private const val QUEUE_NOTIFICATION_PREFIX = "queue:notification"
        private const val ENTRY_CHANNEL = "$QUEUE_NOTIFICATION_PREFIX:entry"
        private const val POSITION_CHANNEL = "$QUEUE_NOTIFICATION_PREFIX:position"
        private const val ACTIVATED_CHANNEL = "$QUEUE_NOTIFICATION_PREFIX:activated"
        private const val EXPIRED_CHANNEL = "$QUEUE_NOTIFICATION_PREFIX:expired"
        private const val BATCH_CHANNEL = "$QUEUE_NOTIFICATION_PREFIX:batch"
    }

    override suspend fun notifyQueueEntry(
        tokenId: String,
        userId: String,
        concertId: Long,
        position: Long,
        estimatedWaitTime: Int
    ) {
        withContext(Dispatchers.IO) {
            try {
                val notification = QueueNotificationMessage(
                    type = "QUEUE_ENTRY",
                    tokenId = tokenId,
                    userId = userId,
                    concertId = concertId,
                    position = position,
                    estimatedWaitTime = estimatedWaitTime,
                    message = "대기열에 진입했습니다",
                    timestamp = System.currentTimeMillis()
                )

                val messageJson = objectMapper.writeValueAsString(notification)
                redisTemplate.convertAndSend(ENTRY_CHANNEL, messageJson)

                log.debug("대기열 진입 알림 발송: tokenId=$tokenId, position=$position")

            } catch (e: Exception) {
                log.error("대기열 진입 알림 발송 실패: tokenId=$tokenId", e)
            }
        }
    }

    override suspend fun notifyPositionUpdate(
        tokenId: String,
        userId: String,
        concertId: Long,
        newPosition: Long,
        estimatedWaitTime: Int
    ) {
        withContext(Dispatchers.IO) {
            try {
                val notification = QueueNotificationMessage(
                    type = "POSITION_UPDATE",
                    tokenId = tokenId,
                    userId = userId,
                    concertId = concertId,
                    position = newPosition,
                    estimatedWaitTime = estimatedWaitTime,
                    message = "대기열 순서가 업데이트되었습니다",
                    timestamp = System.currentTimeMillis()
                )

                val messageJson = objectMapper.writeValueAsString(notification)
                redisTemplate.convertAndSend(POSITION_CHANNEL, messageJson)

                log.debug("위치 업데이트 알림 발송: tokenId=$tokenId, position=$newPosition")

            } catch (e: Exception) {
                log.error("위치 업데이트 알림 발송 실패: tokenId=$tokenId", e)
            }
        }
    }

    override suspend fun notifyTokenActivated(
        tokenId: String,
        userId: String,
        concertId: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val notification = QueueNotificationMessage(
                    type = "TOKEN_ACTIVATED",
                    tokenId = tokenId,
                    userId = userId,
                    concertId = concertId,
                    position = 0L,
                    estimatedWaitTime = 0,
                    message = "🎉 예약이 가능합니다!",
                    timestamp = System.currentTimeMillis()
                )

                val messageJson = objectMapper.writeValueAsString(notification)
                redisTemplate.convertAndSend(ACTIVATED_CHANNEL, messageJson)

                log.info("토큰 활성화 알림 발송: tokenId=$tokenId")

            } catch (e: Exception) {
                log.error("토큰 활성화 알림 발송 실패: tokenId=$tokenId", e)
            }
        }
    }

    override suspend fun notifyTokenExpired(
        tokenId: String,
        userId: String,
        concertId: Long,
        reason: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val notification = QueueNotificationMessage(
                    type = "TOKEN_EXPIRED",
                    tokenId = tokenId,
                    userId = userId,
                    concertId = concertId,
                    position = -1L,
                    estimatedWaitTime = 0,
                    message = reason,
                    timestamp = System.currentTimeMillis()
                )

                val messageJson = objectMapper.writeValueAsString(notification)
                redisTemplate.convertAndSend(EXPIRED_CHANNEL, messageJson)

                log.info("토큰 만료 알림 발송: tokenId=$tokenId, reason=$reason")

            } catch (e: Exception) {
                log.error("토큰 만료 알림 발송 실패: tokenId=$tokenId", e)
            }
        }
    }

    override suspend fun notifyBatchPositionUpdates(
        concertId: Long,
        updates: List<QueuePositionUpdate>
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (updates.isEmpty()) return@withContext

                val batchNotification = BatchQueueNotificationMessage(
                    type = "BATCH_POSITION_UPDATE",
                    concertId = concertId,
                    updates = updates,
                    message = "대기열 순서가 일괄 업데이트되었습니다",
                    timestamp = System.currentTimeMillis()
                )

                val messageJson = objectMapper.writeValueAsString(batchNotification)
                redisTemplate.convertAndSend(BATCH_CHANNEL, messageJson)

                log.info("배치 위치 업데이트 알림 발송: concertId=$concertId, count=${updates.size}")

            } catch (e: Exception) {
                log.error("배치 위치 업데이트 알림 발송 실패: concertId=$concertId", e)
            }
        }
    }
}

/**
 * 개별 알림 메시지
 */
data class QueueNotificationMessage(
    val type: String,
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val position: Long,
    val estimatedWaitTime: Int,
    val message: String,
    val timestamp: Long
)

/**
 * 배치 알림 메시지
 */
data class BatchQueueNotificationMessage(
    val type: String,
    val concertId: Long,
    val updates: List<QueuePositionUpdate>,
    val message: String,
    val timestamp: Long
)