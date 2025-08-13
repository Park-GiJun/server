package kr.hhplus.be.server.infrastructure.adapter.out.persistence.lock

import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
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
            val lockAcquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)

            if (!lockAcquired) {
                throw RuntimeException("락 획득 실패: $lockKey")
            }

            return action()

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock()
            }
        }
    }
}