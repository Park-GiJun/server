package kr.hhplus.be.server.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.SeatStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
@DisplayName("TestContainers를 활용한 Lock 동시성 테스트")
class LockConcurrencyIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("hhplus_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
                "--innodb-lock-wait-timeout=10",
                "--innodb-deadlock-detect=ON"
            )

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.show-sql") { "true" }
            registry.add("spring.jpa.properties.hibernate.format_sql") { "true" }
            registry.add("logging.level.org.hibernate.SQL") { "DEBUG" }
            registry.add("logging.level.org.hibernate.type.descriptor.sql.BasicBinder") { "TRACE" }
        }
    }

    @Autowired
    private lateinit var chargeUserPointUseCase: ChargeUserPointUseCase

    @Autowired
    private lateinit var tempReservationUseCase: TempReservationUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertSeatRepository: ConcertSeatRepository

    @Autowired
    private lateinit var tempReservationRepository: TempReservationRepository

    @BeforeEach
    fun setUp() {
        setupTestData()
    }

    private fun setupTestData() {
        val testUser = User("test-user", "테스트유저", 10000, 10000, 0, LocalDateTime.now(),LocalDateTime.now())
        userRepository.save(testUser)

        val testSeat = ConcertSeat(100L, 100L, "A1", "VIP", SeatStatus.AVAILABLE,LocalDateTime.now(),LocalDateTime.now())
        concertSeatRepository.save(testSeat)

        repeat(10) { i ->
            val user = User("user-test-$i", "유저$i", 5000, 5000, 0,LocalDateTime.now(),LocalDateTime.now())
            userRepository.save(user)
        }

        Thread.sleep(100000);
    }

    @Test
    @DisplayName("사용자 포인트 충전 동시성 테스트 - Pessimistic Lock")
    fun testConcurrentUserPointCharge() {
        val userId = "test-user"
        val chargeAmount = 1000
        val concurrentRequests = 20
        val executor = Executors.newFixedThreadPool(concurrentRequests)

        println("=== 포인트 충전 동시성 테스트 시작 ===")
        println("초기 포인트: ${userRepository.findByUserId(userId)?.availablePoint}")

        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val results = ConcurrentLinkedQueue<Int>()
        val exceptions = ConcurrentLinkedQueue<Exception>()

        val startTime = System.currentTimeMillis()

        repeat(concurrentRequests) { index ->
            executor.submit {
                try {
                    println("스레드 $index 시작 - 포인트 충전 요청")
                    val result = chargeUserPointUseCase.chargeUserPoint(
                        ChargeUserPointCommand(userId, chargeAmount)
                    )
                    results.add(result.totalPoint)
                    successCount.incrementAndGet()
                    println("스레드 $index 성공 - 현재 포인트: ${result.totalPoint}")
                } catch (e: Exception) {
                    exceptions.add(e)
                    failureCount.incrementAndGet()
                    println("스레드 $index 실패 - ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        val finalUser = userRepository.findByUserId(userId)
        val expectedFinalAmount = 10000 + (chargeAmount * concurrentRequests)

        println("=== 포인트 충전 테스트 결과 ===")
        println("완료 여부: $completed")
        println("소요 시간: ${duration}ms")
        println("성공 횟수: ${successCount.get()}")
        println("실패 횟수: ${failureCount.get()}")
        println("최종 포인트: ${finalUser?.totalPoint}")
        println("기대 포인트: $expectedFinalAmount")
        println("예외 목록: ${exceptions.map { "${it.javaClass.simpleName}: ${it.message}" }}")

        assertThat(completed).isTrue()
        assertThat(successCount.get()).isEqualTo(concurrentRequests)
        assertThat(failureCount.get()).isEqualTo(0)
        assertThat(finalUser?.totalPoint).isEqualTo(expectedFinalAmount)
        assertThat(finalUser?.availablePoint).isEqualTo(expectedFinalAmount)
    }

    @Test
    @DisplayName("좌석 예약 동시성 테스트 - 하나의 좌석에 대한 동시 예약")
    fun testConcurrentSeatReservation() {
        val seatId = 1L
        val concurrentUsers = 10
        val executor = Executors.newFixedThreadPool(concurrentUsers)

        println("=== 좌석 예약 동시성 테스트 시작 ===")
        println("초기 좌석 상태: ${concertSeatRepository.findByConcertSeatId(seatId)?.seatStatus}")

        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val successfulUsers = ConcurrentLinkedQueue<String>()
        val exceptions = ConcurrentLinkedQueue<String>()

        val startTime = System.currentTimeMillis()

        repeat(concurrentUsers) { index ->
            executor.submit {
                val userId = "user-$index"
                try {
                    println("사용자 $userId - 좌석 예약 시도")
                    val result = tempReservationUseCase.tempReservation(
                        TempReservationCommand("mock-token-$index", userId, seatId)
                    )
                    successfulUsers.add(userId)
                    successCount.incrementAndGet()
                    println("사용자 $userId - 예약 성공: ${result.tempReservationId}")
                } catch (e: Exception) {
                    exceptions.add("$userId: ${e.javaClass.simpleName} - ${e.message}")
                    failureCount.incrementAndGet()
                    println("사용자 $userId - 예약 실패: ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)

        println("=== 좌석 예약 테스트 결과 ===")
        println("완료 여부: $completed")
        println("소요 시간: ${duration}ms")
        println("성공 횟수: ${successCount.get()}")
        println("실패 횟수: ${failureCount.get()}")
        println("성공한 사용자: ${successfulUsers.toList()}")
        println("최종 좌석 상태: ${finalSeat?.seatStatus}")
        println("예외 목록: ${exceptions.toList()}")

        assertThat(completed).isTrue()
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failureCount.get()).isEqualTo(concurrentUsers - 1)
        assertThat(finalSeat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
        assertThat(successfulUsers).hasSize(1)
    }

    @Test
    @DisplayName("다중 사용자 포인트 충전 테스트 - 각각 다른 사용자")
    fun testMultipleUserConcurrentPointCharge() {
        val userCount = 10
        val chargeAmount = 500
        val requestsPerUser = 5
        val totalRequests = userCount * requestsPerUser
        val executor = Executors.newFixedThreadPool(totalRequests)

        println("=== 다중 사용자 포인트 충전 테스트 시작 ===")

        val latch = CountDownLatch(totalRequests)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val userResults = ConcurrentHashMap<String, MutableList<Int>>()

        val startTime = System.currentTimeMillis()

        repeat(userCount) { userIndex ->
            repeat(requestsPerUser) { requestIndex ->
                executor.submit {
                    val userId = "user-$userIndex"
                    try {
                        val result = chargeUserPointUseCase.chargeUserPoint(
                            ChargeUserPointCommand(userId, chargeAmount)
                        )
                        userResults.computeIfAbsent(userId) { mutableListOf() }.add(result.totalPoint)
                        successCount.incrementAndGet()
                        println("$userId 요청 $requestIndex 성공 - 포인트: ${result.totalPoint}")
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                        println("$userId 요청 $requestIndex 실패 - ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        val completed = latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        println("=== 다중 사용자 테스트 결과 ===")
        println("완료 여부: $completed")
        println("소요 시간: ${duration}ms")
        println("성공 횟수: ${successCount.get()}")
        println("실패 횟수: ${failureCount.get()}")

        userResults.forEach { (userId, results) ->
            val finalUser = userRepository.findByUserId(userId)
            val expectedAmount = 5000 + (chargeAmount * requestsPerUser)
            println("$userId - 결과: $results, 최종: ${finalUser?.totalPoint}, 기대: $expectedAmount")
            assertThat(finalUser?.totalPoint).isEqualTo(expectedAmount)
        }

        assertThat(completed).isTrue()
        assertThat(successCount.get()).isEqualTo(totalRequests)
        assertThat(failureCount.get()).isEqualTo(0)
    }
}