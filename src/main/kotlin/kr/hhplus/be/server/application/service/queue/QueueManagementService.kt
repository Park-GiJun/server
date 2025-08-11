package kr.hhplus.be.server.application.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.ZoneOffset


// src/main/kotlin/kr/hhplus/be/server/infrastructure/adapter/out/persistence/queue/redis/RedisQueueManagementService.kt
@Service
class RedisQueueManagementService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(RedisQueueManagementService::class.java)

    /**
     * 대기열에 사용자 추가 (원자적 처리)
     */
    fun addToWaitingQueue(token: QueueToken): Long {
        val queueKey = token.getWaitingQueueKey()
        val timestamp = token.enteredAt.toInstant(ZoneOffset.UTC).toEpochMilli().toDouble()

        // Lua 스크립트로 원자적 처리
        val script = """
            local queueKey = KEYS[1]
            local userId = ARGV[1]
            local timestamp = tonumber(ARGV[2])
            
            -- 이미 존재하는지 확인
            local existingScore = redis.call('ZSCORE', queueKey, userId)
            if existingScore then
                return redis.call('ZRANK', queueKey, userId)
            end
            
            -- 새로 추가
            redis.call('ZADD', queueKey, timestamp, userId)
            return redis.call('ZRANK', queueKey, userId)
        """.trimIndent()

        val rank = redisTemplate.execute(
            RedisScript.of(script, Long::class.java),
            listOf(queueKey),
            token.userId,
            timestamp
        ) as Long? ?: -1L

        log.info("대기열 추가: userId=${token.userId}, concertId=${token.concertId}, rank=$rank")
        return rank
    }

    /**
     * 대기열 위치 조회
     */
    fun getWaitingPosition(concertId: Long, userId: String): Long {
        val queueKey = "queue:waiting:$concertId"
        return redisTemplate.opsForZSet().rank(queueKey, userId) ?: -1L
    }

    /**
     * 대기열에서 활성화 (배치 처리)
     */
    fun activateWaitingUsers(concertId: Long, count: Int): List<String> {
        val queueKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        val script = """
            local queueKey = KEYS[1]
            local activeKey = KEYS[2]
            local maxActivate = tonumber(ARGV[1])
            local maxActiveTotal = tonumber(ARGV[2])
            
            local currentActive = redis.call('SCARD', activeKey)
            if currentActive >= maxActiveTotal then
                return {}
            end
            
            local toActivate = math.min(maxActivate, maxActiveTotal - currentActive)
            if toActivate <= 0 then
                return {}
            end
            
            local users = redis.call('ZRANGE', queueKey, 0, toActivate - 1)
            
            if #users > 0 then
                redis.call('ZREM', queueKey, unpack(users))
                for i = 1, #users do
                    redis.call('SADD', activeKey, users[i])
                    redis.call('EXPIRE', activeKey, 1800)
                end
            end
            
            return users
        """.trimIndent()

        val result = redisTemplate.execute(
            RedisScript.of(script, List::class.java),
            listOf(queueKey, activeKey),
            count,
            RedisQueueDomainService.MAX_ACTIVE_USERS_PER_CONCERT
        ) as? List<String> ?: emptyList()

        log.info("사용자 활성화: concertId=$concertId, 활성화된 수=${result.size}")
        return result
    }

    /**
     * 활성 사용자 제거
     */
    fun deactivateUser(concertId: Long, userId: String): Boolean {
        val activeKey = "queue:active:$concertId"
        val removed = redisTemplate.opsForSet().remove(activeKey, userId) ?: 0

        log.info("사용자 비활성화: concertId=$concertId, userId=$userId, removed=${removed > 0}")
        return removed > 0
    }

    /**
     * 통계 조회
     */
    fun getQueueStats(concertId: Long): QueueStats {
        val queueKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        return QueueStats(
            concertId = concertId,
            waitingCount = redisTemplate.opsForZSet().zCard(queueKey) ?: 0,
            activeCount = redisTemplate.opsForSet().size(activeKey) ?: 0
        )
    }

    data class QueueStats(
        val concertId: Long,
        val waitingCount: Long,
        val activeCount: Long
    )
}