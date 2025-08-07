package kr.hhplus.be.server.integration

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.user.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.users.User
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("사용자 포인트 동시성 테스트 (Clean)")
class CleanUserPointConcurrencyTest {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("hhplus_test")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.datasource.hikari.maximum-pool-size") { "20" }
        }
    }

    @Autowired
    private lateinit var chargeUserPointUseCase: ChargeUserPointUseCase

    @Autowired
    private lateinit var useUserPointUseCase: UseUserPointUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun createTestUser(userId: String, initialPoint: Int = 100000): User {
        val user = User(
            userId = userId,
            userName = "테스트유저$userId",
            totalPoint = initialPoint,
            availablePoint = initialPoint,
            usedPoint = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(user)
    }

    @Test
    @DisplayName("동일 사용자 포인트 충전 동시성 테스트")
    fun `concurrent_point_charge_same_user`() = runTest {
        // Given
        val userId = "charge-test-${System.currentTimeMillis()}"
        val testUser = createTestUser(userId)

        val chargeAmount = 1000
        val concurrentRequests = 20

        val successCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)

        println("=== 포인트 충전 동시성 테스트 시작 ===")
        println("사용자: $userId")
        println("초기 포인트: ${testUser.totalPoint}")

        // When - 동시 충전 요청
        val jobs = (1..concurrentRequests).map { index ->
            async(Dispatchers.IO) {
                try {
                    val result = chargeUserPointUseCase.chargeUserPoint(
                        ChargeUserPointCommand(userId, chargeAmount)
                    )
                    successCount.incrementAndGet()
                    println("[$index] 충전 성공 - 현재 포인트: ${result.totalPoint}")
                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[$index] 낙관적 락 실패")
                } catch (e: Exception) {
                    println("[$index] 기타 실패: ${e.message}")
                }
            }
        }

        jobs.awaitAll()

        // Then - Awaitility로 최종 상태 검증
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            val finalUser = userRepository.findByUserId(userId)!!
            val expectedPoint = testUser.totalPoint + (chargeAmount * successCount.get())
            assertThat(finalUser.totalPoint).isEqualTo(expectedPoint)
        }

        val finalUser = userRepository.findByUserId(userId)!!

        println("\n=== 🎯 충전 동시성 테스트 결과 ===")
        println("성공: ${successCount.get()}")
        println("낙관적 락 실패: ${optimisticLockFailures.get()}")
        println("💰 최종 포인트: ${finalUser.totalPoint}")

        // 검증
        assertThat(successCount.get()).isGreaterThan(0)
        // 비관적 락 사용 시 모든 요청이 순차적으로 성공할 수 있음
        assertThat(successCount.get()).isLessThanOrEqualTo(concurrentRequests)
    }

    @Test
    @DisplayName("동일 사용자 포인트 사용 동시성 테스트")
    fun `concurrent_point_use_same_user`() = runTest {
        // Given
        val userId = "use-test-${System.currentTimeMillis()}"
        val testUser = createTestUser(userId, 50000)

        val useAmount = 2000
        val concurrentRequests = 15

        val successCount = AtomicInteger(0)
        val insufficientPointFailures = AtomicInteger(0)

        println("=== 포인트 사용 동시성 테스트 시작 ===")
        println("사용자: $userId")
        println("초기 포인트: ${testUser.availablePoint}")

        // When - 동시 사용 요청
        val jobs = (1..concurrentRequests).map { index ->
            async(Dispatchers.IO) {
                try {
                    val result = useUserPointUseCase.useUserPoint(
                        UseUserPointCommand(userId, useAmount)
                    )
                    successCount.incrementAndGet()
                    println("[$index] 사용 성공 - 잔여: ${result.availablePoint}")
                } catch (e: IllegalArgumentException) {
                    insufficientPointFailures.incrementAndGet()
                    println("[$index] 💸 잔액 부족")
                } catch (e: Exception) {
                    println("[$index] 실패: ${e.message}")
                }
            }
        }

        jobs.awaitAll()

        // Then
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            val finalUser = userRepository.findByUserId(userId)!!
            assertThat(finalUser.availablePoint).isGreaterThanOrEqualTo(0)
        }

        val finalUser = userRepository.findByUserId(userId)!!

        println("\n=== 🎯 사용 동시성 테스트 결과 ===")
        println("성공: ${successCount.get()}")
        println("💸 잔액 부족: ${insufficientPointFailures.get()}")
        println("💰 최종 사용가능: ${finalUser.availablePoint}")

        // 검증
        assertThat(finalUser.availablePoint).isGreaterThanOrEqualTo(0)
        assertThat(successCount.get()).isLessThanOrEqualTo(testUser.availablePoint / useAmount)
    }

    @Test
    @DisplayName("포인트 충전/사용 혼합 동시성 테스트")
    fun `concurrent_mixed_operations`() = runTest {
        // Given
        val userId = "mixed-test-${System.currentTimeMillis()}"
        val testUser = createTestUser(userId, 50000)

        val operationCount = 50
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        println("=== 혼합 작업 동시성 테스트 시작 ===")

        // When - 랜덤 충전/사용 작업
        val jobs = (1..operationCount).map { index ->
            async(Dispatchers.IO) {
                try {
                    val amount = Random.nextInt(1000, 3000)
                    if (Random.nextBoolean()) {
                        // 충전
                        chargeUserPointUseCase.chargeUserPoint(
                            ChargeUserPointCommand(userId, amount)
                        )
                        println("[$index] 충전 $amount")
                    } else {
                        // 사용
                        useUserPointUseCase.useUserPoint(
                            UseUserPointCommand(userId, amount)
                        )
                        println("[$index] 사용 $amount")
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    println("[$index] 실패: ${e.message}")
                }
            }
        }

        jobs.awaitAll()

        // Then
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            val finalUser = userRepository.findByUserId(userId)!!
            assertThat(finalUser.totalPoint).isGreaterThan(0)
            assertThat(finalUser.availablePoint).isGreaterThanOrEqualTo(0)
        }

        val finalUser = userRepository.findByUserId(userId)!!

        println("\n=== 🎯 혼합 작업 결과 ===")
        println("성공: ${successCount.get()}")
        println("실패: ${failureCount.get()}")
        println("💰 최종 총 포인트: ${finalUser.totalPoint}")
        println("💰 사용가능: ${finalUser.availablePoint}")

        // 검증
        assertThat(successCount.get()).isGreaterThan(0)
        assertThat(finalUser.availablePoint + finalUser.usedPoint).isEqualTo(finalUser.totalPoint)
    }
}