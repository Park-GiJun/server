package kr.hhplus.be.server

import kotlinx.coroutines.*
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class DistributedLockDebugTest : IntegrationTestBase() {

    private val log = LoggerFactory.getLogger(DistributedLockDebugTest::class.java)

    @Autowired
    private lateinit var distributedLockPort: DistributedLockPort

    @Test
    @DisplayName("분산 락 기본 동작 확인")
    fun testBasicLockOperation() {
        // given
        val lockKey = "test:lock:basic"
        val counter = AtomicInteger(0)

        // when
        val result = distributedLockPort.executeWithLock(
            lockKey = lockKey,
            waitTime = 5L,
            leaseTime = 10L
        ) {
            counter.incrementAndGet()
            log.info("Lock acquired and executed")
            "success"
        }

        // then
        assertThat(result).isEqualTo("success")
        assertThat(counter.get()).isEqualTo(1)
    }

    @Test
    @DisplayName("동시 락 요청 처리 확인")
    fun testConcurrentLockRequests() {
        runBlocking {
            // given
            val lockKey = "test:lock:concurrent"
            val counter = AtomicInteger(0)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // when
            val jobs = (1..5).map { index ->
                async(Dispatchers.IO) {
                    try {
                        distributedLockPort.executeWithLock(
                            lockKey = lockKey,
                            waitTime = 10L,
                            leaseTime = 1L
                        ) {
                            val count = counter.incrementAndGet()
                            log.info("Thread $index acquired lock, counter: $count")
                            Thread.sleep(100) // 작업 시뮬레이션
                            successCount.incrementAndGet()
                            count
                        }
                    } catch (e: Exception) {
                        log.error("Thread $index failed: ${e.message}")
                        failCount.incrementAndGet()
                        -1
                    }
                }
            }

            val results = jobs.awaitAll()

            // then
            log.info("Results: Success=${successCount.get()}, Fail=${failCount.get()}, Counter=${counter.get()}")

            // 최소 1개는 성공해야 함
            assertThat(successCount.get()).isGreaterThan(0)

            // 카운터 값과 성공 횟수가 일치해야 함
            assertThat(counter.get()).isEqualTo(successCount.get())
        }
    }

    @Test
    @DisplayName("서로 다른 락은 동시 처리 가능")
    fun testDifferentLocksConcurrent() {
        runBlocking {
            // given
            val lockKey1 = "test:lock:key1"
            val lockKey2 = "test:lock:key2"
            val counter1 = AtomicInteger(0)
            val counter2 = AtomicInteger(0)

            // when
            val job1 = async(Dispatchers.IO) {
                distributedLockPort.executeWithLock(
                    lockKey = lockKey1,
                    waitTime = 5L,
                    leaseTime = 10L
                ) {
                    Thread.sleep(100)
                    counter1.incrementAndGet()
                    log.info("Lock1 executed")
                    "result1"
                }
            }

            val job2 = async(Dispatchers.IO) {
                distributedLockPort.executeWithLock(
                    lockKey = lockKey2,
                    waitTime = 5L,
                    leaseTime = 10L
                ) {
                    Thread.sleep(100)
                    counter2.incrementAndGet()
                    log.info("Lock2 executed")
                    "result2"
                }
            }

            val results = awaitAll(job1, job2)

            // then
            assertThat(results).containsExactlyInAnyOrder("result1", "result2")
            assertThat(counter1.get()).isEqualTo(1)
            assertThat(counter2.get()).isEqualTo(1)
        }
    }
}