package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis.RedisQueueManagementService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class QueueTokenRedisAdapter(
    private val queueManagementService: RedisQueueManagementService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate,
    private val redisQueueDomainService: RedisQueueDomainService,
    private val objectMapper: ObjectMapper
) : QueueTokenRepository {

    private val log = LoggerFactory.getLogger(QueueTokenRedisAdapter::class.java)



    override fun save(token: QueueToken): QueueToken {
        val entity = QueueTokenRedisEntity.fromDomain(token)
        val key = token.getRedisKey()

        // Hash로 토큰 정보 저장
        val entityMap = objectMapper.convertValue(entity, object : TypeReference<Map<String, Any>>() {})
        redisTemplate.opsForHash<String, Any>().putAll(key, entityMap)

        // TTL 설정
        val ttl = redisQueueDomainService.calculateTTL(token.tokenStatus)
        redisTemplate.expire(key, ttl)

        // 사용자별 토큰 매핑 저장
        stringRedisTemplate.opsForValue().set(token.getUserKey(), token.queueTokenId, ttl)

        // 대기 상태면 대기열에 추가
        if (token.isWaiting()) {
            queueManagementService.addToWaitingQueue(token)
        }

        log.info("Redis 토큰 저장: tokenId=${token.queueTokenId}, status=${token.tokenStatus}")
        return token
    }

    override fun findByTokenId(tokenId: String): QueueToken? {
        val key = "queue:token:$tokenId"
        val entityMap = redisTemplate.opsForHash<String, Any>().entries(key)

        return if (entityMap.isEmpty()) {
            null
        } else {
            try {
                val entity = objectMapper.convertValue(entityMap, QueueTokenRedisEntity::class.java)
                entity.toDomain()
            } catch (e: Exception) {
                log.error("토큰 역직렬화 실패: tokenId=$tokenId", e)
                null
            }
        }
    }

    override fun findActiveTokenByUserAndConcert(userId: String, concertId: Long): QueueToken? {
        val userKey = "queue:user:$userId:$concertId"
        val tokenId = stringRedisTemplate.opsForValue().get(userKey)

        return tokenId?.let {
            findByTokenId(it)?.takeIf { token ->
                token.tokenStatus in listOf(QueueTokenStatus.WAITING, QueueTokenStatus.ACTIVE)
            }
        }
    }

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        val userKey = "queue:user:$userId:$concertId"
        val tokenId = stringRedisTemplate.opsForValue().get(userKey)
        return tokenId?.let { findByTokenId(it) }
    }

    override fun countWaitingTokensBeforeUser(userId: String, concertId: Long, enteredAt: LocalDateTime): Int {
        val position = queueManagementService.getWaitingPosition(concertId, userId)
        return if (position >= 0) position.toInt() else 0
    }

    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        val userIds = queueManagementService.activateWaitingUsers(concertId, count)

        return userIds.mapNotNull { userId ->
            findByUserIdAndConcertId(userId, concertId)?.let { token ->
                if (token.isWaiting()) {
                    val activatedToken = token.activate()
                    save(activatedToken)
                    activatedToken
                } else token
            }
        }
    }

    override fun findWaitingTokensByConcert(concertId: Long): List<QueueToken> {
        return emptyList()
    }

    override fun countActiveTokensByConcert(concertId: Long): Int {
        val stats = queueManagementService.getQueueStats(concertId)
        return stats.activeCount.toInt()
    }
}