package kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
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
    private val queueManagementService: RedisQueueManagementRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate,
    private val redisQueueDomainService: RedisQueueDomainService,
    private val objectMapper: ObjectMapper
) : QueueTokenRepository {

    private val log = LoggerFactory.getLogger(QueueTokenRedisAdapter::class.java)

    override fun save(token: QueueToken): QueueToken {
        val entity = QueueTokenRedisEntity.fromDomain(token)
        val key = "queue:token:${token.queueTokenId}"

        // Hash로 토큰 정보 저장
        val entityMap = objectMapper.convertValue(entity, object : TypeReference<Map<String, Any>>() {})
        redisTemplate.opsForHash<String, Any>().putAll(key, entityMap)

        // TTL 설정
        val ttl = redisQueueDomainService.calculateTTL(token.tokenStatus)
        redisTemplate.expire(key, ttl)

        // 사용자별 토큰 매핑 저장
        val userKey = "queue:user:${token.userId}:${token.concertId}"
        stringRedisTemplate.opsForValue().set(userKey, token.queueTokenId, ttl)

        // 대기 상태면 대기열에 추가
        if (token.tokenStatus == QueueTokenStatus.WAITING) {
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

    override fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken? {
        val userKey = "queue:user:$userId:$concertId"
        val tokenId = stringRedisTemplate.opsForValue().get(userKey)
        return tokenId?.let { findByTokenId(it) }
    }

    override fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken> {
        log.info("토큰 활성화 시작: concertId=$concertId, count=$count")

        // 1. Redis에서 대기 중인 사용자들을 활성화
        val activatedUserIds = queueManagementService.activateWaitingUsers(concertId, count)

        if (activatedUserIds.isEmpty()) {
            log.info("활성화할 대기 중인 사용자 없음: concertId=$concertId")
            return emptyList()
        }

        // 2. 각 사용자의 토큰을 조회하고 상태 업데이트
        val activatedTokens = mutableListOf<QueueToken>()

        activatedUserIds.forEach { userId ->
            val token = findByUserIdAndConcertId(userId, concertId)

            if (token != null && token.tokenStatus == QueueTokenStatus.WAITING) {
                // 토큰 상태를 ACTIVE로 변경
                val activatedToken = token.copy(
                    tokenStatus = QueueTokenStatus.ACTIVE,
                    expiresAt = LocalDateTime.now().plusMinutes(30)
                )

                save(activatedToken)
                activatedTokens.add(activatedToken)

                log.info("토큰 활성화 완료: tokenId=${activatedToken.queueTokenId}, userId=$userId")
            }
        }

        log.info("토큰 활성화 완료: concertId=$concertId, 활성화=${activatedTokens.size}개")
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
        // 활성 큐에서 사용자 목록 조회
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
        log.info("토큰 상태 업데이트: tokenId=$tokenId, ${token.tokenStatus} -> $status")

        return updatedToken
    }

    override fun deleteToken(tokenId: String): Boolean {
        val token = findByTokenId(tokenId) ?: return false

        redisTemplate.delete("queue:token:$tokenId")

        stringRedisTemplate.delete("queue:user:${token.userId}:${token.concertId}")

        queueManagementService.removeFromAllQueues(token.concertId, token.userId)

        log.info("토큰 삭제 완료: tokenId=$tokenId")
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

        log.info("활성 토큰 만료 처리: tokenId=$tokenId, reason=$reason")
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

        log.info("배치 상태 업데이트 완료: 요청=${tokenIds.size}개, 성공=${updatedCount}개")
        return updatedCount
    }
}