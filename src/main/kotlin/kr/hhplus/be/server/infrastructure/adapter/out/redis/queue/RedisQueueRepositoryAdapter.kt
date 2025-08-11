package kr.hhplus.be.server.infrastructure.adapter.out.redis.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.queue.QueueRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * Redis 기반 대기열 저장소 어댑터
 * - Redis Sorted Set으로 대기 순서 관리
 * - Redis Set으로 활성 토큰 관리
 * - Redis Hash로 토큰 상세 정보 저장
 */
@Component
class RedisQueueRepositoryAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : QueueRepository {

    companion object {
        // Redis 키 패턴
        private const val QUEUE_WAITING_PREFIX = "queue:waiting:"       // Sorted Set
        private const val QUEUE_ACTIVE_PREFIX = "queue:active:"         // Set
        private const val TOKEN_DATA_PREFIX = "token:data:"             // Hash
        private const val USER_TOKEN_PREFIX = "user:token:"             // String
    }

    override suspend fun addToQueue(token: QueueToken): Long {
        return withContext(Dispatchers.IO) {
            val concertId = token.concertId
            val queueKey = "$QUEUE_WAITING_PREFIX$concertId"
            val tokenDataKey = "$TOKEN_DATA_PREFIX${token.tokenId}"
            val userTokenKey = "$USER_TOKEN_PREFIX${token.userId}:$concertId"

            // 1. 토큰 데이터 저장 (Hash)
            val tokenData = mapOf(
                "tokenId" to token.tokenId,
                "userId" to token.userId,
                "concertId" to token.concertId.toString(),
                "status" to token.status.name,
                "enteredAt" to token.enteredAt.toString()
            )
            redisTemplate.opsForHash<String, Any>().putAll(tokenDataKey, tokenData)

            // 2. 사용자별 토큰 매핑 저장
            redisTemplate.opsForValue().set(userTokenKey, token.tokenId)

            // 3. 대기열에 추가 (timestamp를 score로 사용)
            val timestamp = System.currentTimeMillis().toDouble()
            redisTemplate.opsForZSet().add(queueKey, token.tokenId, timestamp)

            // 4. 현재 위치 반환 (0부터 시작)
            redisTemplate.opsForZSet().rank(queueKey, token.tokenId) ?: 0L
        }
    }

    override suspend fun findByTokenId(tokenId: String): QueueToken? {
        return withContext(Dispatchers.IO) {
            val tokenDataKey = "$TOKEN_DATA_PREFIX$tokenId"
            val data = redisTemplate.opsForHash<String, String>().entries(tokenDataKey)

            if (data.isEmpty()) {
                null
            } else {
                QueueToken(
                    tokenId = data["tokenId"] ?: tokenId,
                    userId = data["userId"] ?: "",
                    concertId = data["concertId"]?.toLongOrNull() ?: 0L,
                    status = QueueTokenStatus.valueOf(data["status"] ?: "WAITING"),
                    enteredAt = LocalDateTime.parse(data["enteredAt"] ?: LocalDateTime.now().toString())
                )
            }
        }
    }

    override suspend fun findByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        return withContext(Dispatchers.IO) {
            val userTokenKey = "$USER_TOKEN_PREFIX$userId:$concertId"
            val tokenId = redisTemplate.opsForValue().get(userTokenKey) as? String
                ?: return@withContext null

            findByTokenId(tokenId)
        }
    }

    override suspend fun getQueuePosition(tokenId: String): Long? {
        return withContext(Dispatchers.IO) {
            val token = findByTokenId(tokenId) ?: return@withContext null
            val queueKey = "$QUEUE_WAITING_PREFIX${token.concertId}"

            redisTemplate.opsForZSet().rank(queueKey, tokenId)
        }
    }

    override suspend fun activateToken(tokenId: String, ttl: Duration) {
        withContext(Dispatchers.IO) {
            val token = findByTokenId(tokenId) ?: return@withContext
            val concertId = token.concertId

            val queueKey = "$QUEUE_WAITING_PREFIX$concertId"
            val activeKey = "$QUEUE_ACTIVE_PREFIX$concertId"
            val tokenDataKey = "$TOKEN_DATA_PREFIX$tokenId"

            // 1. 대기열에서 제거
            redisTemplate.opsForZSet().remove(queueKey, tokenId)

            // 2. 활성 토큰으로 추가
            redisTemplate.opsForSet().add(activeKey, tokenId)
            redisTemplate.expire(activeKey, ttl)

            // 3. 토큰 상태 업데이트
            redisTemplate.opsForHash<String, Any>().put(tokenDataKey, "status", QueueTokenStatus.ACTIVE.name)
            redisTemplate.opsForHash<String, Any>().put(tokenDataKey, "activatedAt", LocalDateTime.now().toString())
        }
    }

    override suspend fun updateTokenStatus(tokenId: String, status: QueueTokenStatus) {
        withContext(Dispatchers.IO) {
            val tokenDataKey = "$TOKEN_DATA_PREFIX$tokenId"
            redisTemplate.opsForHash<String, Any>().put(tokenDataKey, "status", status.name)

            if (status == QueueTokenStatus.EXPIRED) {
                redisTemplate.opsForHash<String, Any>().put(tokenDataKey, "expiredAt", LocalDateTime.now().toString())
            }
        }
    }

    override suspend fun removeToken(tokenId: String) {
        withContext(Dispatchers.IO) {
            val token = findByTokenId(tokenId) ?: return@withContext
            val concertId = token.concertId

            val queueKey = "$QUEUE_WAITING_PREFIX$concertId"
            val activeKey = "$QUEUE_ACTIVE_PREFIX$concertId"
            val tokenDataKey = "$TOKEN_DATA_PREFIX$tokenId"
            val userTokenKey = "$USER_TOKEN_PREFIX${token.userId}:$concertId"

            // 모든 관련 키에서 제거
            redisTemplate.opsForZSet().remove(queueKey, tokenId)
            redisTemplate.opsForSet().remove(activeKey, tokenId)
            redisTemplate.delete(tokenDataKey)
            redisTemplate.delete(userTokenKey)
        }
    }

    override suspend fun countActiveTokens(concertId: Long): Int {
        return withContext(Dispatchers.IO) {
            val activeKey = "$QUEUE_ACTIVE_PREFIX$concertId"
            (redisTemplate.opsForSet().size(activeKey) ?: 0L).toInt()
        }
    }

    override suspend fun countWaitingTokens(concertId: Long): Long {
        return withContext(Dispatchers.IO) {
            val queueKey = "$QUEUE_WAITING_PREFIX$concertId"
            redisTemplate.opsForZSet().count(queueKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        }
    }

    override suspend fun activateNextTokens(concertId: Long, count: Int, ttl: Duration): List<String> {
        return withContext(Dispatchers.IO) {
            val queueKey = "$QUEUE_WAITING_PREFIX$concertId"

            // 대기열에서 다음 순서 토큰들 조회
            val tokensToActivate = redisTemplate.opsForZSet().range(queueKey, 0, count - 1L)
                ?.filterIsInstance<String>() ?: emptyList()

            // 각 토큰을 활성화
            tokensToActivate.forEach { tokenId ->
                activateToken(tokenId, ttl)
            }

            tokensToActivate
        }
    }

    override suspend fun cleanupExpiredTokens(): Int {
        return withContext(Dispatchers.IO) {
            var cleanedCount = 0

            // 만료된 토큰들 찾기 및 정리 로직
            // 실제 구현에서는 TTL 만료된 키들을 스캔하여 정리

            cleanedCount
        }
    }
}