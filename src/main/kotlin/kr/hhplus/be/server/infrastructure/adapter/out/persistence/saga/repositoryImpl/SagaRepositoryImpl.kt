package kr.hhplus.be.server.infrastructure.adapter.out.persistence.saga.repositoryImpl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.port.out.saga.SagaRepository
import kr.hhplus.be.server.application.saga.payment.PaymentSagaContext
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * Redis 기반 Saga Repository 구현체
 * 기존 UserRepositoryImpl, ReservationRepositoryImpl과 동일한 패턴
 */
@Repository
class SagaRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : SagaRepository {

    private val log = LoggerFactory.getLogger(SagaRepositoryImpl::class.java)

    companion object {
        private const val SAGA_KEY_PREFIX = "saga:payment:"
        private const val SAGA_TTL_HOURS = 24L
    }

    override fun save(context: PaymentSagaContext) {
        try {
            val key = buildSagaKey(context.sagaId)
            val contextJson = objectMapper.writeValueAsString(context)

            redisTemplate.opsForValue().set(key, contextJson, Duration.ofHours(SAGA_TTL_HOURS))
            log.debug("Saga context saved: ${context.sagaId}, state: ${context.state}")

        } catch (e: Exception) {
            log.error("Failed to save saga context: ${context.sagaId}", e)
            throw RuntimeException("Failed to save saga context", e)
        }
    }

    override fun findById(sagaId: String): PaymentSagaContext? {
        return try {
            val key = buildSagaKey(sagaId)
            val contextJson = redisTemplate.opsForValue().get(key) as String?

            contextJson?.let {
                val context = objectMapper.readValue(it, PaymentSagaContext::class.java)
                log.debug("Saga context found: $sagaId, state: ${context.state}")
                context
            } ?: run {
                log.debug("Saga context not found: $sagaId")
                null
            }

        } catch (e: Exception) {
            log.error("Failed to find saga context: $sagaId", e)
            null
        }
    }

    override fun delete(sagaId: String) {
        try {
            val key = buildSagaKey(sagaId)
            val deleted = redisTemplate.delete(key)

            if (deleted) {
                log.debug("Saga context deleted: $sagaId")
            } else {
                log.warn("Saga context not found for deletion: $sagaId")
            }

        } catch (e: Exception) {
            log.error("Failed to delete saga context: $sagaId", e)
        }
    }

    override fun getActiveSagaCount(): Long {
        return try {
            val keys = redisTemplate.keys("$SAGA_KEY_PREFIX*")
            keys?.size?.toLong() ?: 0L

        } catch (e: Exception) {
            log.error("Failed to get active saga count", e)
            0L
        }
    }

    override fun findAllActive(): List<PaymentSagaContext> {
        return try {
            val keys = redisTemplate.keys("$SAGA_KEY_PREFIX*") ?: emptySet()

            keys.mapNotNull { key ->
                try {
                    val contextJson = redisTemplate.opsForValue().get(key) as String?
                    contextJson?.let {
                        objectMapper.readValue(it, PaymentSagaContext::class.java)
                    }
                } catch (e: Exception) {
                    log.warn("Failed to deserialize saga from key: $key", e)
                    null
                }
            }.filter {
                // 완료되지 않은 Saga만 반환
                it.state != PaymentSagaState.COMPLETED && it.state != PaymentSagaState.FAILED
            }

        } catch (e: Exception) {
            log.error("Failed to find all active sagas", e)
            emptyList()
        }
    }

    private fun buildSagaKey(sagaId: String): String {
        return SAGA_KEY_PREFIX + sagaId
    }
}