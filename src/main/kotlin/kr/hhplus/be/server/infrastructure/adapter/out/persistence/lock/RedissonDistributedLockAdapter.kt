package kr.hhplus.be.server.infrastructure.adapter.out.persistence.lock

import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.domain.lock.exception.LockAcquisitionException
import kr.hhplus.be.server.domain.lock.exception.LockTimeoutException
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
            log.debug("락 획득 시도: key={}, waitTime={}초, leaseTime={}초", lockKey, waitTime, leaseTime)

            val lockAcquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)

            if (!lockAcquired) {
                log.warn("락 획득 타임아웃: key={}, waitTime={}초", lockKey, waitTime)
                throw LockTimeoutException(lockKey, waitTime)
            }

            log.debug("락 획득 성공: key={}", lockKey)
            return action()

        } catch (e: InterruptedException) {
            log.error("락 획득 중 인터럽트 발생: key={}", lockKey, e)
            Thread.currentThread().interrupt()
            throw LockAcquisitionException(lockKey, e)
        } catch (e: Exception) {
            if (e is LockTimeoutException || e is LockAcquisitionException) {
                throw e
            }
            log.error("락 획득 중 예외 발생: key={}", lockKey, e)
            throw LockAcquisitionException(lockKey, e)
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock()
                    log.debug("락 해제 완료: key={}", lockKey)
                }
            } catch (e: Exception) {
                log.error("락 해제 중 예외 발생: key={}", lockKey, e)
            }
        }
    }
}