package kr.hhplus.be.server.infrastructure.adapter.out.persistence.lock

import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.domain.lock.exception.ConcurrencyProcessingException
import kr.hhplus.be.server.domain.lock.exception.LockAcquisitionException
import kr.hhplus.be.server.domain.lock.exception.LockTimeoutException
import kr.hhplus.be.server.domain.lock.exception.ResourceBusyException
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedissonDistributedLockAdapter(
    private val redissonClient: RedissonClient
) : DistributedLockPort {

    private val log = LoggerFactory.getLogger(RedissonDistributedLockAdapter::class.java)

    override fun <T> executeWithLock(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        action: () -> T
    ): T {
        val lock = redissonClient.getLock(lockKey)

        try {
            log.debug("락 획득 시도: key={}", lockKey)

            val lockAcquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)

            if (!lockAcquired) {
                log.warn("락 획득 실패 - 즉시 실패 처리: key={}", lockKey)
                throw ResourceBusyException("현재 처리 중인 요청이 있습니다.")
            }

            log.debug("락 획득 성공: key={}", lockKey)
            return action()

        } catch (e: InterruptedException) {
            log.error("락 획득 중 인터럽트: key={}", lockKey, e)
            Thread.currentThread().interrupt()
            throw ConcurrencyProcessingException("요청 처리 중 중단되었습니다.")
        } catch (e: ResourceBusyException) {
            throw e
        } catch (e: Exception) {
            log.error("락 처리 중 예외: key={}", lockKey, e)
            throw ConcurrencyProcessingException("동시성 처리 중 오류가 발생했습니다.")
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock()
                    log.debug("락 해제 완료: key={}", lockKey)
                }
            } catch (e: Exception) {
                log.error("락 해제 중 예외: key={}", lockKey, e)
            }
        }
    }
}