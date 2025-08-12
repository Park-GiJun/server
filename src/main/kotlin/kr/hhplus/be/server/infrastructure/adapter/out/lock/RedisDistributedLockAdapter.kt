package kr.hhplus.be.server.infrastructure.adapter.out.lock

import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.infrastructure.adapter.out.lock.exception.DistributedLockAcquisitionException
import kr.hhplus.be.server.infrastructure.adapter.out.lock.exception.DistributedLockException
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.application.dto.lock.LockEvent
import kr.hhplus.be.server.application.dto.lock.LockType
import kr.hhplus.be.server.application.port.out.lock.event.LockEventPort
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RedisDistributedLockAdapter(
    private val redissonClient: RedissonClient,
    private val lockEventPort: LockEventPort
) : DistributedLockPort {

    private val log = LoggerFactory.getLogger(RedisDistributedLockAdapter::class.java)

    override fun tryLock(key: String, waitTime: Duration, leaseTime: Duration): Boolean {
        val lock = redissonClient.getLock(key)

        return try {
            val acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)
            if (acquired) {
                log.debug("분산락 획득 성공: key=$key")
            } else {
                log.warn("분산락 획득 실패: key=$key, waitTime=${waitTime.toMillis()}ms")
            }
            acquired
        } catch (e: InterruptedException) {
            log.error("분산락 획득 중 인터럽트: key=$key", e)
            Thread.currentThread().interrupt()
            false
        } catch (e: Exception) {
            log.error("분산락 획득 중 예외: key=$key", e)
            false
        }
    }

    override fun unlock(key: String) {
        val lock = redissonClient.getLock(key)

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock()
                log.debug("분산락 해제 완료: key=$key")
            } else {
                log.warn("현재 스레드가 소유하지 않은 락 해제 시도: key=$key")
            }
        } catch (e: Exception) {
            log.error("분산락 해제 중 예외: key=$key", e)
        }
    }

    override fun <T> withLock(key: String, waitTime: Duration, leaseTime: Duration, action: () -> T): T {
        val lockType = determineLockType(key)
        return withLock(key, lockType, waitTime, leaseTime, action)
    }

    override fun <T> withLock(
        key: String,
        lockType: LockType,
        waitTime: Duration,
        leaseTime: Duration,
        action: () -> T
    ): T {
        val lock = redissonClient.getLock(key)
        val holderId = generateHolderId()
        val startTime = System.currentTimeMillis()

        try {
            val acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)
            val lockAcquireTime = System.currentTimeMillis() - startTime

            if (!acquired) {
                log.warn("분산락 획득 실패: key=$key, 대기시간=${lockAcquireTime}ms")

                publishEventSafely {
                    lockEventPort.publishLockEvent(
                        LockEvent.failed(key, lockType, holderId)
                    )
                }

                throw DistributedLockAcquisitionException("분산락 획득 실패: key=$key")
            }

            log.debug("분산락 작업 시작: key=$key, 획득시간=${lockAcquireTime}ms")

            publishEventSafely {
                lockEventPort.publishLockEvent(
                    LockEvent.acquired(key, lockType, holderId)
                )
            }

            val actionStartTime = System.currentTimeMillis()
            val result = action()
            val actionTime = System.currentTimeMillis() - actionStartTime

            log.debug("분산락 작업 완료: key=$key, 실행시간=${actionTime}ms")
            return result

        } catch (e: InterruptedException) {
            log.error("분산락 작업 중 인터럽트: key=$key", e)
            Thread.currentThread().interrupt()
            throw DistributedLockException("분산락 작업 중 인터럽트 발생", e)
        } catch (e: DistributedLockAcquisitionException) {
            throw e
        } catch (e: Exception) {
            log.error("분산락 작업 중 예외: key=$key", e)
            throw DistributedLockException("분산락 작업 중 예외 발생", e)
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock()
                    log.debug("분산락 해제: key=$key")

                    publishEventSafely {
                        lockEventPort.publishLockEvent(
                            LockEvent.released(key, lockType)
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("분산락 해제 중 예외: key=$key", e)
            }

            val totalTime = System.currentTimeMillis() - startTime
            log.debug("분산락 전체 처리시간: key=$key, 총시간=${totalTime}ms")
        }
    }

    private fun determineLockType(key: String): LockType {
        return when {
            key.contains(":temp_reservation:seat:") -> LockType.TEMP_RESERVATION_SEAT
            key.contains(":temp_reservation:process:") -> LockType.TEMP_RESERVATION_PROCESS
            key.contains(":payment:user:") -> LockType.PAYMENT_USER
            key.contains(":point:balance:") -> LockType.POINT_BALANCE
            key.contains(":seat:status:") -> LockType.SEAT_STATUS
            key.contains(":queue:activation:") -> LockType.QUEUE_ACTIVATION
            else -> {
                log.warn("알 수 없는 락 키 패턴: $key, 기본값 사용")
                LockType.TEMP_RESERVATION_SEAT
            }
        }
    }

    private fun generateHolderId(): String {
        return "${Thread.currentThread().name}-${System.currentTimeMillis()}"
    }

    private fun publishEventSafely(publishAction: suspend () -> Unit) {
        try {
            runBlocking {
                publishAction()
            }
        } catch (e: Exception) {
            log.warn("락 이벤트 발행 실패", e)
        }
    }
}