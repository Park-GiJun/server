package kr.hhplus.be.server.infrastructure.adapter.out.event.lock

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.dto.lock.LockEvent
import kr.hhplus.be.server.application.dto.lock.LockEventType
import kr.hhplus.be.server.application.dto.lock.LockType
import kr.hhplus.be.server.application.port.out.lock.event.LockEventPort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisLockEventAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : LockEventPort {

    private val log = LoggerFactory.getLogger(RedisLockEventAdapter::class.java)

    companion object {
        private const val LOCK_EVENT_CHANNEL = "lock:events"
    }

    override suspend fun publishLockEvent(event: LockEvent) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            redisTemplate.convertAndSend(LOCK_EVENT_CHANNEL, eventJson)

            log.debug("락 이벤트 발행: type=${event.eventType}, key=${event.lockKey}")
        } catch (e: Exception) {
            log.error("락 이벤트 발행 실패: $event", e)
        }
    }

    override suspend fun publishLockReleased(lockKey: String, lockType: LockType) {
        val event = LockEvent(
            lockKey = lockKey,
            lockType = lockType,
            eventType = LockEventType.RELEASED
        )
        publishLockEvent(event)
    }

    override suspend fun publishLockAcquired(lockKey: String, lockType: LockType, holderId: String) {
        val event = LockEvent(
            lockKey = lockKey,
            lockType = lockType,
            eventType = LockEventType.ACQUIRED,
            holderId = holderId
        )
        publishLockEvent(event)
    }
}
