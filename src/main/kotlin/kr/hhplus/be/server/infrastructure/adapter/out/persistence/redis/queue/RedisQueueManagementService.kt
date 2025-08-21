package kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.dto.QueueStats
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class RedisQueueManagementService(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    private val log = LoggerFactory.getLogger(RedisQueueManagementService::class.java)

    fun addToWaitingQueue(token: QueueToken): Long {
        val queueKey = "queue:waiting:${token.concertId}"
        val timestamp = token.enteredAt.toInstant(ZoneOffset.UTC).toEpochMilli().toDouble()

        val existingRank = redisTemplate.opsForZSet().rank(queueKey, token.userId)
        if (existingRank != null) {
            log.info("이미 대기열에 존재: userId=${token.userId}, rank=$existingRank")
            return existingRank
        }

        redisTemplate.opsForZSet().add(queueKey, token.userId, timestamp)
        val rank = redisTemplate.opsForZSet().rank(queueKey, token.userId) ?: -1L

        log.info("대기열 추가: userId=${token.userId}, concertId=${token.concertId}, rank=$rank")
        return rank
    }

    fun getWaitingPosition(concertId: Long, userId: String): Long {
        val queueKey = "queue:waiting:$concertId"
        return redisTemplate.opsForZSet().rank(queueKey, userId) ?: -1L
    }

    fun getNextWaitingUsers(concertId: Long, count: Int): List<String> {
        val waitingKey = "queue:waiting:$concertId"

        return redisTemplate.opsForZSet()
            .range(waitingKey, 0, (count - 1).toLong())
            ?.map { it.toString() } ?: emptyList()
    }

    fun activateWaitingUsers(concertId: Long, count: Int): List<String> {
        val waitingKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        val usersToActivate = redisTemplate.opsForZSet()
            .range(waitingKey, 0, (count - 1).toLong())
            ?.map { it.toString() } ?: emptyList()

        if (usersToActivate.isEmpty()) {
            return emptyList()
        }

        val currentTime = System.currentTimeMillis() / 1000
        val expiryTime = currentTime + 1800 // 30분 후 만료

        usersToActivate.forEach { userId ->
            redisTemplate.opsForZSet().remove(waitingKey, userId)
            redisTemplate.opsForZSet().add(activeKey, userId, expiryTime.toDouble())
        }

        log.info("사용자 활성화: concertId=$concertId, 활성화수=${usersToActivate.size}")
        return usersToActivate
    }

    fun getQueueStats(concertId: Long): QueueStats {
        val waitingKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        val waitingCount = redisTemplate.opsForZSet().zCard(waitingKey) ?: 0L
        val activeCount = redisTemplate.opsForZSet().zCard(activeKey) ?: 0L

        return QueueStats(
            concertId = concertId,
            waitingCount = waitingCount,
            activeCount = activeCount
        )
    }

    fun removeFromAllQueues(concertId: Long, userId: String) {
        val waitingKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        val waitingRemoved = redisTemplate.opsForZSet().remove(waitingKey, userId) ?: 0L
        val activeRemoved = redisTemplate.opsForZSet().remove(activeKey, userId) ?: 0L

        val totalRemoved = waitingRemoved + activeRemoved
        if (totalRemoved > 0) {
            log.info("큐에서 사용자 제거: concertId=$concertId, userId=$userId, 제거수=$totalRemoved")
        }
    }

    fun cleanupExpiredActiveTokens(concertId: Long): List<String> {
        val activeKey = "queue:active:$concertId"
        val currentTime = System.currentTimeMillis() / 1000

        val expiredUsers = redisTemplate.opsForZSet()
            .rangeByScore(activeKey, Double.NEGATIVE_INFINITY, currentTime.toDouble())
            ?.map { it.toString() } ?: emptyList()

        if (expiredUsers.isNotEmpty()) {
            redisTemplate.opsForZSet().removeRangeByScore(
                activeKey,
                Double.NEGATIVE_INFINITY,
                currentTime.toDouble()
            )

            log.info("만료된 활성 토큰 정리: concertId=$concertId, 정리수=${expiredUsers.size}")
        }

        return expiredUsers
    }

}