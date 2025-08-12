package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis

import kr.hhplus.be.server.domain.queue.QueueToken
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/**
 * 간단한 Redis 대기열 관리 서비스
 * - LUA 스크립트 없이 단순 Redis 명령어만 사용
 * - 필수 기능만 구현
 */
@Service
class RedisQueueManagementService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate
) {

    private val log = LoggerFactory.getLogger(RedisQueueManagementService::class.java)

    /**
     * 대기열에 사용자 추가
     */
    fun addToWaitingQueue(token: QueueToken): Long {
        val queueKey = "queue:waiting:${token.concertId}"
        val timestamp = token.enteredAt.toInstant(ZoneOffset.UTC).toEpochMilli().toDouble()

        // 이미 존재하는지 확인
        val existingRank = redisTemplate.opsForZSet().rank(queueKey, token.userId)
        if (existingRank != null) {
            log.info("이미 대기열에 존재: userId=${token.userId}, rank=$existingRank")
            return existingRank
        }

        // 새로 추가
        redisTemplate.opsForZSet().add(queueKey, token.userId, timestamp)
        val rank = redisTemplate.opsForZSet().rank(queueKey, token.userId) ?: -1L

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
     * 대기열의 다음 N명 조회
     */
    fun getNextWaitingUsers(concertId: Long, count: Int): List<String> {
        val waitingKey = "queue:waiting:$concertId"

        return redisTemplate.opsForZSet()
            .range(waitingKey, 0, (count - 1).toLong())
            ?.map { it.toString() } ?: emptyList()
    }

    /**
     * 대기열의 다음 N명을 활성 큐로 이동
     */
    fun activateWaitingUsers(concertId: Long, count: Int): List<String> {
        val waitingKey = "queue:waiting:$concertId"
        val activeKey = "queue:active:$concertId"

        // 1. 대기열에서 상위 N명 조회
        val usersToActivate = redisTemplate.opsForZSet()
            .range(waitingKey, 0, (count - 1).toLong())
            ?.map { it.toString() } ?: emptyList()

        if (usersToActivate.isEmpty()) {
            return emptyList()
        }

        val currentTime = System.currentTimeMillis() / 1000
        val expiryTime = currentTime + 1800 // 30분 후 만료

        // 2. 각 사용자를 대기열에서 제거하고 활성 큐에 추가
        usersToActivate.forEach { userId ->
            // 대기열에서 제거
            redisTemplate.opsForZSet().remove(waitingKey, userId)
            // 활성 큐에 추가 (만료 시간을 score로 사용)
            redisTemplate.opsForZSet().add(activeKey, userId, expiryTime.toDouble())
        }

        log.info("사용자 활성화: concertId=$concertId, 활성화수=${usersToActivate.size}")
        return usersToActivate
    }

    /**
     * 큐 통계 조회
     */
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

    /**
     * 모든 큐에서 사용자 제거
     */
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

    /**
     * 만료된 활성 토큰들 정리
     */
    fun cleanupExpiredActiveTokens(concertId: Long): List<String> {
        val activeKey = "queue:active:$concertId"
        val currentTime = System.currentTimeMillis() / 1000

        // 만료된 토큰들 조회
        val expiredUsers = redisTemplate.opsForZSet()
            .rangeByScore(activeKey, Double.NEGATIVE_INFINITY, currentTime.toDouble())
            ?.map { it.toString() } ?: emptyList()

        if (expiredUsers.isNotEmpty()) {
            // 만료된 토큰들 제거
            redisTemplate.opsForZSet().removeRangeByScore(
                activeKey,
                Double.NEGATIVE_INFINITY,
                currentTime.toDouble()
            )

            log.info("만료된 활성 토큰 정리: concertId=$concertId, 정리수=${expiredUsers.size}")
        }

        return expiredUsers
    }

    /**
     * 활성 토큰 존재 여부 확인
     */
    fun isActiveToken(concertId: Long, userId: String): Boolean {
        val activeKey = "queue:active:$concertId"
        val score = redisTemplate.opsForZSet().score(activeKey, userId)

        if (score == null) return false

        // TTL 확인 (현재 시간과 비교)
        val currentTime = System.currentTimeMillis() / 1000
        return score > currentTime
    }
}

/**
 * 큐 통계 데이터 클래스
 */
data class QueueStats(
    val concertId: Long,
    val waitingCount: Long,
    val activeCount: Long
)