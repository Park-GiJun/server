package kr.hhplus.be.server.infrastructure.adapter.out.persistence.lock

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.domain.lock.exception.ResourceBusyException
import kr.hhplus.be.server.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class RedissonDistributedLockIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var distributedLockPort: DistributedLockPort

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @BeforeEach
    fun setUp() {
        // 모든 락 해제
        try {
            redissonClient.keys.flushall()
        } catch (e: Exception) {
            // Redis가 아직 준비되지 않은 경우 무시
        }
    }

    @Test
    @DisplayName("단일 스레드에서 락 획득 및 해제가 정상 작동한다")
    fun `single thread lock acquisition and release works correctly`() {
        // given
        val lockKey = "test:lock:single"
        val result = mutableListOf<String>()

        // when
        val returnValue = distributedLockPort.executeWithLock(
            lockKey = lockKey,
            waitTime = 5L,
            leaseTime = 10L
        ) {
            result.add("executed")
            "success"
        }

        // then
        assertThat(returnValue).isEqualTo("success")
        assertThat(result).containsExactly("executed")

        // 락이 해제되었는지 확인
        val lock = redissonClient.getLock(lockKey)
        assertThat(lock.isLocked).isFalse()
    }

    @Test
    @DisplayName("동시에 여러 스레드가 같은 락을 요청하면 순차적으로 처리된다")
    fun `concurrent threads requesting same lock are processed sequentially`() = runTest {
        // given
        val lockKey = "test:lock:concurrent"
        val counter = AtomicInteger(0)
        val executionOrder = ConcurrentHashMap<Int, Long>()
        val threadCount = 10

        // when
        val jobs = (1..threadCount).map { index ->
            async(Dispatchers.IO) {
                try {
                    distributedLockPort.executeWithLock(
                        lockKey = lockKey,
                        waitTime = 10L,
                        leaseTime = 1L
                    ) {
                        val order = counter.incrementAndGet()
                        executionOrder[index] = order.toLong()
                        Thread.sleep(50) // 작업 시뮬레이션
                        order
                    }
                } catch (e: ResourceBusyException) {
                    -1
                }
            }
        }

        val results = jobs.awaitAll()

        // then
        val successfulExecutions = results.filter { it > 0 }

        // 순차적으로 실행되었는지 확인
        assertThat(successfulExecutions).isSorted()

        // 최소한 일부는 성공해야 함
        assertThat(successfulExecutions).isNotEmpty()

        println("성공한 실행 수: ${successfulExecutions.size}/$threadCount")
    }

    @Test
    @DisplayName("서로 다른 락 키는 동시에 처리될 수 있다")
    fun `different lock keys can be processed concurrently`() = runTest {
        // given
        val lockKey1 = "test:lock:key1"
        val lockKey2 = "test:lock:key2"
        val startLatch = CountDownLatch(2)
        val endLatch = CountDownLatch(2)

        // when
        val time = measureTimeMillis {
            val job1 = async(Dispatchers.IO) {
                distributedLockPort.executeWithLock(
                    lockKey = lockKey1,
                    waitTime = 5L,
                    leaseTime = 10L
                ) {
                    startLatch.countDown()
                    Thread.sleep(100)
                    endLatch.countDown()
                    "result1"
                }
            }

            val job2 = async(Dispatchers.IO) {
                distributedLockPort.executeWithLock(
                    lockKey = lockKey2,
                    waitTime = 5L,
                    leaseTime = 10L
                ) {
                    startLatch.countDown()
                    Thread.sleep(100)
                    endLatch.countDown()
                    "result2"
                }
            }

            awaitAll(job1, job2)
        }

        // then
        // 동시에 실행되므로 200ms보다 훨씬 적게 걸려야 함
        assertThat(time).isLessThan(200)
        assertThat(startLatch.count).isEqualTo(0)
        assertThat(endLatch.count).isEqualTo(0)
    }

    @Test
    @DisplayName("락 내부에서 예외가 발생해도 락이 정상적으로 해제된다")
    fun `lock is released even when exception occurs inside lock`() {
        // given
        val lockKey = "test:lock:exception"

        // when
        assertThrows<RuntimeException> {
            distributedLockPort.executeWithLock(
                lockKey = lockKey,
                waitTime = 5L,
                leaseTime = 10L
            ) {
                throw RuntimeException("Test exception")
            }
        }

        // then
        val lock = redissonClient.getLock(lockKey)
        assertThat(lock.isLocked).isFalse()
    }
}