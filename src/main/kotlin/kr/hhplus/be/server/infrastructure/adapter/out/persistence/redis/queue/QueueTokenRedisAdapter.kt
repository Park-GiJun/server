package kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.entity.QueueTokenRedisEntity
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
        val key = "queue:token:${token.queueTokenId}"

        val entityMap = objectMapper.convertValue(entity, object : TypeReference<Map<String, Any>>() {})
        redisTemplate.opsForHash<String, Any>().putAll(key, entityMap)

        val ttl = redisQueueDomainService.calculateTTL(token.tokenStatus)
        redisTemplate.expire(key, ttl)

        val userKey = "queue:user:${token.userId}:${token.concertId}"
        stringRedisTemplate.opsForValue().set(userKey, token.queueTokenId, ttl)

        if (token.tokenStatus == QueueTokenStatus.WAITING) {
            queueManagementService.addToWaitingQueue(token)
        }
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
                null
            }
        }
    }

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        val userKey = "queue:user:$userId:$concertId"
        val tokenId = stringRedisTemplate.opsForValue().get(userKey)
        return tokenId?.let { findByTokenId(it) }
    }

    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        val activatedUserIds = queueManagementService.activateWaitingUsers(concertId, count)

        if (activatedUserIds.isEmpty()) {
            return emptyList()
        }
        val activatedTokens = mutableListOf<QueueToken>()

        activatedUserIds.forEach { userId ->
            val token = findByUserIdAndConcertId(userId, concertId)

            if (token != null && token.tokenStatus == QueueTokenStatus.WAITING) {
                val activatedToken = token.copy(
                    tokenStatus = QueueTokenStatus.ACTIVE,
                    expiresAt = LocalDateTime.now().plusMinutes(30)
                )

                save(activatedToken)
                activatedTokens.add(activatedToken)
            }
        }
        return activatedTokens
    }

    override fun findWaitingTokensByConcert(concertId: Long, limit: Int): List<QueueToken> {
        val waitingUsers = queueManagementService.getNextWaitingUsers(concertId, limit)

        return waitingUsers.mapNotNull { userId ->
            findByUserIdAndConcertId(userId, concertId)?.takeIf {
                it.tokenStatus == QueueTokenStatus.WAITING
            }
        }
    }

    override fun findActiveTokensByConcert(concertId: Long): List<QueueToken> {
        val activeKey = "queue:active:$concertId"
        val activeUsers = redisTemplate.opsForZSet()
            .range(activeKey, 0, -1)
            ?.map { it.toString() } ?: emptyList()

        return activeUsers.mapNotNull { userId ->
            findByUserIdAndConcertId(userId, concertId)?.takeIf {
                it.tokenStatus == QueueTokenStatus.ACTIVE
            }
        }
    }

    override fun findExpiredTokensByConcert(concertId: Long?): List<QueueToken> {
        if (concertId != null) {
            val expiredActiveUsers = queueManagementService.cleanupExpiredActiveTokens(concertId)
            return expiredActiveUsers.mapNotNull { userId ->
                findByUserIdAndConcertId(userId, concertId)
            }
        }
        return emptyList()
    }

    override fun updateTokenStatus(tokenId: String, status: QueueTokenStatus): QueueToken? {
        val token = findByTokenId(tokenId) ?: return null

        val updatedToken = token.copy(
            tokenStatus = status,
            expiresAt = if (status == QueueTokenStatus.ACTIVE)
                LocalDateTime.now().plusMinutes(30) else token.expiresAt
        )
        save(updatedToken)
        return updatedToken
    }

    override fun deleteToken(tokenId: String): Boolean {
        val token = findByTokenId(tokenId) ?: return false

        redisTemplate.delete("queue:token:$tokenId")

        stringRedisTemplate.delete("queue:user:${token.userId}:${token.concertId}")

        queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        return true
    }

    override fun countTokensByStatusAndConcert(concertId: Long): Map<QueueTokenStatus, Int> {
        val stats = queueManagementService.getQueueStats(concertId)

        return mapOf(
            QueueTokenStatus.WAITING to stats.waitingCount.toInt(),
            QueueTokenStatus.ACTIVE to stats.activeCount.toInt(),
            QueueTokenStatus.COMPLETED to 0,
            QueueTokenStatus.EXPIRED to 0
        )
    }

    override fun expireActiveToken(tokenId: String, reason: String): QueueToken? {
        val token = findByTokenId(tokenId) ?: return null

        if (token.tokenStatus != QueueTokenStatus.ACTIVE) {
            return null
        }

        val expiredToken = token.copy(
            tokenStatus = QueueTokenStatus.EXPIRED,
            expiresAt = LocalDateTime.now()
        )

        save(expiredToken)
        queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        return expiredToken
    }

    override fun hasActiveToken(userId: String, concertId: Long): Boolean {
        val token = findByUserIdAndConcertId(userId, concertId)
        return token?.tokenStatus == QueueTokenStatus.ACTIVE
    }

    override fun batchUpdateTokenStatus(tokenIds: List<String>, status: QueueTokenStatus): Int {
        var updatedCount = 0

        tokenIds.forEach { tokenId ->
            updateTokenStatus(tokenId, status)?.let {
                updatedCount++
            }
        }
        return updatedCount
    }
}