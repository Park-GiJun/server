package kr.hhplus.be.server.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.user.UseUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity

import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
@DisplayName("Awaitility를 활용한 낙관적 락 동시성 테스트")
class AwaitilityOptimisticLockTest {

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
                "--innodb-lock-wait-timeout=5",
                "--innodb-deadlock-detect=ON"
            )

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.show-sql") { "false" } // 성능을 위해 로그 최소화
        }
    }

    @Autowired private lateinit var chargeUserPointUseCase: ChargeUserPointUseCase
    @Autowired private lateinit var useUserPointUseCase: UseUserPointUseCase
    @Autowired private lateinit var tempReservationUseCase: TempReservationUseCase
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var concertSeatRepository: ConcertSeatRepository
    @Autowired private lateinit var tempReservationRepository: TempReservationRepository
    @Autowired private lateinit var queueTokenRepository: QueueTokenRepository

    private val executorService = Executors.newFixedThreadPool(20)

    @BeforeEach
    fun setUp() {
        setupTestData()
    }

    private fun setupTestData() {
        // 테스트 사용자들을 Repository를 통해 저장 (JpaEntity로 변환되어 @Version 적용됨)
        repeat(10) { i ->
            val user = User(
                userId = "user-$i",
                userName = "테스트유저$i",
                totalPoint = 10000,
                availablePoint = 10000,
                usedPoint = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(user) // 이때 UserJpaEntity로 변환되어 @Version이 적용됨
        }

        // 활성 토큰들 생성
        repeat(10) { i ->
            val token = QueueToken(
                queueTokenId = "token-$i",
                userId = "user-$i",
                concertId = 1L,
                tokenStatus = QueueTokenStatus.ACTIVE,
                enteredAt = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            queueTokenRepository.save(token)
        }

        // 테스트 좌석들 생성
        repeat(5) { i ->
            val seat = ConcertSeat(
                concertSeatId = (i + 1).toLong(),
                concertDateId = 100L,
                seatNumber = "A${i + 1}",
                seatGrade = "VIP",
                seatStatus = SeatStatus.AVAILABLE,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            concertSeatRepository.save(seat)
        }
    }

    @Test
    @DisplayName("Awaitility - 사용자 포인트 충전 낙관적 락 테스트")
    fun testUserPointChargeOptimisticLock() {
        val userId = "user-0"
        val chargeAmount = 1000
        val concurrentRequests = 15

        val successCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)
        val otherFailures = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)

        println("=== 📈 포인트 충전 낙관적 락 테스트 시작 ===")

        val initialUser = userRepository.findByUserId(userId)!!
        println("초기 포인트: ${initialUser.totalPoint}")
        println("동시 요청 수: $concurrentRequests")

        // 동시에 충전 요청 실행
        repeat(concurrentRequests) { index ->
            executorService.submit {
                try {
                    println("[$index] 포인트 $chargeAmount 충전 시작")

                    val result = chargeUserPointUseCase.chargeUserPoint(
                        ChargeUserPointCommand(userId, chargeAmount)
                    )

                    successCount.incrementAndGet()
                    println("[$index] ✅ 충전 성공: ${result.totalPoint}")

                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[$index] 🔒 낙관적 락 실패")

                } catch (e: Exception) {
                    otherFailures.incrementAndGet()
                    println("[$index] ❌ 기타 실패: ${e.javaClass.simpleName}")
                } finally {
                    completedRequests.incrementAndGet()
                }
            }
        }

        // Awaitility로 모든 요청 완료까지 기다리기
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            assertThat(completedRequests.get()).isEqualTo(concurrentRequests)
        }

        // DB 상태 안정화를 위해 잠시 대기 후 최종 검증
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val currentUser = userRepository.findByUserId(userId)!!
            val expectedPoint = initialUser.totalPoint + (chargeAmount * successCount.get())
            assertThat(currentUser.totalPoint).isEqualTo(expectedPoint)
        }

        val finalUser = userRepository.findByUserId(userId)!!
        val expectedFinalPoint = initialUser.totalPoint + (chargeAmount * successCount.get())

        println("\n=== 💰 포인트 충전 낙관적 락 테스트 결과 ===")
        println("✅ 성공: ${successCount.get()}")
        println("🔒 낙관적 락 실패: ${optimisticLockFailures.get()}")
        println("❌ 기타 실패: ${otherFailures.get()}")
        println("💰 최종 포인트: ${finalUser.totalPoint}")
        println("🎯 기대 포인트: $expectedFinalPoint")

        // 검증: UserJpaEntity의 @Version이 동작해서 낙관적 락 실패가 발생해야 함
        assertThat(successCount.get()).isGreaterThan(0)
        assertThat(optimisticLockFailures.get()).isGreaterThan(0) // 핵심: 낙관적 락 충돌 발생
        assertThat(finalUser.totalPoint).isEqualTo(expectedFinalPoint)
    }

    @Test
    @DisplayName("Awaitility - 포인트 충전과 사용 동시 실행 낙관적 락 테스트")
    fun testPointChargeAndUseOptimisticLock() {
        val userId = "user-1"
        val chargeAmount = 2000
        val useAmount = 800
        val chargeRequests = 8
        val useRequests = 10

        val chargeSuccessCount = AtomicInteger(0)
        val useSuccessCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)
        val otherFailures = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)
        val totalRequests = chargeRequests + useRequests

        println("=== 💸 포인트 충전/사용 동시 낙관적 락 테스트 시작 ===")

        val initialUser = userRepository.findByUserId(userId)!!
        println("초기 포인트: ${initialUser.totalPoint}")
        println("충전 요청: $chargeRequests 개, 사용 요청: $useRequests 개")

        // 충전 요청들
        repeat(chargeRequests) { index ->
            executorService.submit {
                try {
                    println("[CHARGE-$index] 포인트 $chargeAmount 충전 시작")
                    chargeUserPointUseCase.chargeUserPoint(
                        ChargeUserPointCommand(userId, chargeAmount)
                    )
                    chargeSuccessCount.incrementAndGet()
                    println("[CHARGE-$index] ✅ 충전 성공")
                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[CHARGE-$index] 🔒 낙관적 락 실패")
                } catch (e: Exception) {
                    otherFailures.incrementAndGet()
                    println("[CHARGE-$index] ❌ 충전 실패: ${e.message}")
                } finally {
                    completedRequests.incrementAndGet()
                }
            }
        }

        // 사용 요청들
        repeat(useRequests) { index ->
            executorService.submit {
                try {
                    println("[USE-$index] 포인트 $useAmount 사용 시작")
                    useUserPointUseCase.useUserPoint(
                        UseUserPointCommand(userId, useAmount)
                    )
                    useSuccessCount.incrementAndGet()
                    println("[USE-$index] ✅ 사용 성공")
                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[USE-$index] 🔒 낙관적 락 실패")
                } catch (e: Exception) {
                    otherFailures.incrementAndGet()
                    println("[USE-$index] ❌ 사용 실패: ${e.message}")
                } finally {
                    completedRequests.incrementAndGet()
                }
            }
        }

        // Awaitility로 모든 요청 완료 대기
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            assertThat(completedRequests.get()).isEqualTo(totalRequests)
        }

        // 최종 상태 안정화 대기
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val currentUser = userRepository.findByUserId(userId)!!
            val expectedPoint = initialUser.totalPoint +
                    (chargeAmount * chargeSuccessCount.get()) -
                    (useAmount * useSuccessCount.get())
            assertThat(currentUser.totalPoint).isEqualTo(expectedPoint)
        }

        val finalUser = userRepository.findByUserId(userId)!!
        val expectedFinalPoint = initialUser.totalPoint +
                (chargeAmount * chargeSuccessCount.get()) -
                (useAmount * useSuccessCount.get())

        println("\n=== 💰 충전/사용 동시 낙관적 락 테스트 결과 ===")
        println("✅ 충전 성공: ${chargeSuccessCount.get()}")
        println("✅ 사용 성공: ${useSuccessCount.get()}")
        println("🔒 낙관적 락 실패: ${optimisticLockFailures.get()}")
        println("❌ 기타 실패: ${otherFailures.get()}")
        println("💰 최종 포인트: ${finalUser.totalPoint}")
        println("🎯 기대 포인트: $expectedFinalPoint")

        // 검증: 낙관적 락 충돌이 발생했는지 확인
        assertThat(optimisticLockFailures.get()).isGreaterThan(0)
        assertThat(finalUser.totalPoint).isEqualTo(expectedFinalPoint)
        assertThat(finalUser.availablePoint).isEqualTo(expectedFinalPoint)
    }

    @Test
    @DisplayName("Awaitility - 임시 좌석 예약 동시성 테스트")
    fun testTempSeatReservationConcurrency() {
        val seatId = 1L
        val concurrentUsers = 12

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val seatAlreadyBookedCount = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)
        val successfulUserId = ConcurrentLinkedQueue<String>()

        println("=== 🎫 임시 좌석 예약 동시성 테스트 시작 ===")
        println("테스트 좌석 ID: $seatId")
        println("동시 예약 시도 사용자: $concurrentUsers 명")

        // 동시에 좌석 예약 요청
        repeat(concurrentUsers) { index ->
            val userId = "user-${index % 10}" // 기존 사용자 중에서 순환 선택
            val tokenId = "token-${index % 10}"

            executorService.submit {
                try {
                    println("[$userId] 좌석 $seatId 예약 시도")

                    val result = tempReservationUseCase.tempReservation(
                        TempReservationCommand(tokenId, userId, seatId)
                    )

                    successCount.incrementAndGet()
                    successfulUserId.add(userId)
                    println("[$userId] ✅ 예약 성공! ID: ${result.tempReservationId}")

                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    if (e.message?.contains("이미") == true ||
                        e.message?.contains("already") == true) {
                        seatAlreadyBookedCount.incrementAndGet()
                        println("[$userId] 🪑 좌석 이미 예약됨")
                    } else {
                        println("[$userId] ❌ 예약 실패: ${e.javaClass.simpleName}")
                    }
                } finally {
                    completedRequests.incrementAndGet()
                }
            }
        }

        await withPollInterval Duration.ofMillis(50) untilAsserted {
            assertThat(completedRequests.get()).isEqualTo(concurrentUsers)
        }

        await withPollInterval Duration.ofMillis(100) untilAsserted {
            val seat = concertSeatRepository.findByConcertSeatId(seatId)!!
            assertThat(seat.seatStatus).isIn(SeatStatus.RESERVED, SeatStatus.AVAILABLE)
        }

        val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)!!
        val tempReservations = tempReservationRepository.findAll()
            .filter { it.concertSeatId == seatId }

        println("\n=== 🎪 임시 좌석 예약 동시성 테스트 결과 ===")
        println("✅ 예약 성공: ${successCount.get()}")
        println("🪑 좌석 이미 예약됨: ${seatAlreadyBookedCount.get()}")
        println("❌ 기타 실패: ${failureCount.get() - seatAlreadyBookedCount.get()}")
        println("🎯 최종 좌석 상태: ${finalSeat.seatStatus}")
        println("📝 임시예약 수: ${tempReservations.size}")
        println("🏆 성공한 사용자: ${successfulUserId.toList()}")

        // 검증: 정확히 하나의 예약만 성공해야 함
        if (successCount.get() > 0) {
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED)
            assertThat(tempReservations).hasSize(1)
        }
        assertThat(seatAlreadyBookedCount.get()).isGreaterThan(0) // 동시성 제어가 동작했는지
    }

    @Test
    @DisplayName("Awaitility - 여러 사용자 각각 포인트 충전 테스트")
    fun testMultipleUsersPointChargeOptimisticLock() {
        val userIds = listOf("user-2", "user-3", "user-4", "user-5")
        val chargeAmount = 1500
        val requestsPerUser = 5
        val totalRequests = userIds.size * requestsPerUser

        val successCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)
        val userSuccessMap = ConcurrentHashMap<String, AtomicInteger>()

        userIds.forEach { userId ->
            userSuccessMap[userId] = AtomicInteger(0)
        }

        println("=== 👥 여러 사용자 포인트 충전 낙관적 락 테스트 ===")
        println("테스트 사용자: $userIds")
        println("사용자당 요청: $requestsPerUser, 충전 금액: $chargeAmount")

        // 각 사용자별로 동시 충전 요청
        userIds.forEach { userId ->
            repeat(requestsPerUser) { index ->
                executorService.submit {
                    try {
                        println("[$userId-$index] 포인트 충전 시작")

                        chargeUserPointUseCase.chargeUserPoint(
                            ChargeUserPointCommand(userId, chargeAmount)
                        )

                        successCount.incrementAndGet()
                        userSuccessMap[userId]!!.incrementAndGet()
                        println("[$userId-$index] ✅ 충전 성공")

                    } catch (e: OptimisticLockingFailureException) {
                        optimisticLockFailures.incrementAndGet()
                        println("[$userId-$index] 🔒 낙관적 락 실패")

                    } catch (e: Exception) {
                        println("[$userId-$index] ❌ 실패: ${e.message}")
                    } finally {
                        completedRequests.incrementAndGet()
                    }
                }
            }
        }

        // 모든 요청 완료 대기
        await withPollInterval Duration.ofMillis(100) atMost Duration.ofSeconds(30) untilAsserted {
            assertThat(completedRequests.get()).isEqualTo(totalRequests)
        }

        // 각 사용자별 최종 상태 검증
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            userIds.forEach { userId ->
                val user = userRepository.findByUserId(userId)!!
                val expectedPoint = 10000 + (chargeAmount * userSuccessMap[userId]!!.get())
                assertThat(user.totalPoint).isEqualTo(expectedPoint)
            }
        }

        println("\n=== 👨‍👩‍👧‍👦 여러 사용자 충전 테스트 결과 ===")
        println("✅ 총 성공: ${successCount.get()}")
        println("🔒 낙관적 락 실패: ${optimisticLockFailures.get()}")

        userIds.forEach { userId ->
            val finalUser = userRepository.findByUserId(userId)!!
            val userSuccess = userSuccessMap[userId]!!.get()
            val expectedPoint = 10000 + (chargeAmount * userSuccess)

            println("[$userId] 성공: $userSuccess, 최종포인트: ${finalUser.totalPoint}, 기대: $expectedPoint")
            assertThat(finalUser.totalPoint).isEqualTo(expectedPoint)
        }

        // 여러 사용자 간에는 낙관적 락 충돌이 적어야 함 (각자 다른 레코드)
        assertThat(optimisticLockFailures.get()).isLessThan(totalRequests / 2)
    }

    @Test
    @DisplayName("Awaitility - 동일 사용자 고강도 동시성 테스트")
    fun testSingleUserHighConcurrencyOptimisticLock() {
        val userId = "user-6"
        val operationCount = 30
        val chargeAmount = 200
        val useAmount = 150

        val totalSuccessCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)
        val insufficientPointFailures = AtomicInteger(0)
        val completedOperations = AtomicInteger(0)

        println("=== ⚡ 단일 사용자 고강도 동시성 테스트 ===")
        println("테스트 사용자: $userId")
        println("총 작업 수: $operationCount (충전/사용 랜덤)")

        val initialUser = userRepository.findByUserId(userId)!!
        println("초기 포인트: ${initialUser.totalPoint}")

        val startTime = System.currentTimeMillis()

        // 충전과 사용을 무작위로 섞어서 고강도 실행
        repeat(operationCount) { index ->
            executorService.submit {
                try {
                    if (index % 3 == 0) {
                        // 충전
                        println("[$index] 충전 $chargeAmount 시도")
                        chargeUserPointUseCase.chargeUserPoint(
                            ChargeUserPointCommand(userId, chargeAmount)
                        )
                        println("[$index] ✅ 충전 성공")
                    } else {
                        // 사용
                        println("[$index] 사용 $useAmount 시도")
                        useUserPointUseCase.useUserPoint(
                            UseUserPointCommand(userId, useAmount)
                        )
                        println("[$index] ✅ 사용 성공")
                    }
                    totalSuccessCount.incrementAndGet()

                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[$index] 🔒 낙관적 락 실패")

                } catch (e: Exception) {
                    if (e.message?.contains("부족") == true) {
                        insufficientPointFailures.incrementAndGet()
                        println("[$index] 💰 잔액 부족")
                    } else {
                        println("[$index] ❌ 실패: ${e.message}")
                    }
                } finally {
                    completedOperations.incrementAndGet()
                }
            }
        }

        // 모든 작업 완료 대기 (최대 60초)
        await withPollInterval Duration.ofMillis(100) atMost Duration.ofSeconds(60) untilAsserted {
            assertThat(completedOperations.get()).isEqualTo(operationCount)
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val finalUser = userRepository.findByUserId(userId)!!

        println("\n=== ⚡ 고강도 동시성 테스트 결과 ===")
        println("⏱️ 총 소요 시간: ${duration}ms")
        println("🎯 TPS: ${operationCount * 1000.0 / duration}")
        println("✅ 성공: ${totalSuccessCount.get()}")
        println("🔒 낙관적 락 실패: ${optimisticLockFailures.get()}")
        println("💰 잔액 부족: ${insufficientPointFailures.get()}")
        println("📊 성공률: ${totalSuccessCount.get() * 100.0 / operationCount}%")
        println("💰 최종 포인트: ${finalUser.totalPoint}")

        // 검증: 높은 동시성에서 낙관적 락 충돌이 많이 발생해야 함
        assertThat(optimisticLockFailures.get()).isGreaterThan(5) // 최소 5번 이상 충돌
        assertThat(totalSuccessCount.get()).isGreaterThan(0)
        assertThat(finalUser.totalPoint).isGreaterThanOrEqualTo(0)
    }

    @Test
    @DisplayName("Awaitility - 대량 좌석 예약 경쟁 테스트")
    fun testMassiveSeatReservationConcurrency() {
        val seatIds = listOf(1L, 2L, 3L, 4L, 5L)
        val usersPerSeat = 8
        val totalRequests = seatIds.size * usersPerSeat

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)
        val seatWinners = ConcurrentHashMap<Long, String>()

        println("=== 🏟️ 대량 좌석 예약 경쟁 테스트 ===")
        println("테스트 좌석: $seatIds")
        println("좌석당 경쟁자: $usersPerSeat 명, 총 요청: $totalRequests")

        var requestIndex = 0
        seatIds.forEach { seatId ->
            repeat(usersPerSeat) { userIndex ->
                val userId = "user-${requestIndex % 10}"
                val tokenId = "token-${requestIndex % 10}"
                requestIndex++

                executorService.submit {
                    try {
                        println("[$userId] 좌석 $seatId 예약 시도")

                        val result = tempReservationUseCase.tempReservation(
                            TempReservationCommand(tokenId, userId, seatId)
                        )

                        seatWinners[seatId] = userId
                        successCount.incrementAndGet()
                        println("[$userId] ✅ 좌석 $seatId 예약 성공!")

                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                        println("[$userId] ❌ 좌석 $seatId 예약 실패")
                    } finally {
                        completedRequests.incrementAndGet()
                    }
                }
            }
        }

        // 모든 요청 완료 대기
        await withPollInterval Duration.ofMillis(100) atMost Duration.ofSeconds(45) untilAsserted {
            assertThat(completedRequests.get()).isEqualTo(totalRequests)
        }

        // 모든 좌석의 최종 상태 확인 대기
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val reservedSeatsCount = seatIds.count { seatId ->
                val seat = concertSeatRepository.findByConcertSeatId(seatId)
                seat?.seatStatus == SeatStatus.RESERVED
            }
            assertThat(reservedSeatsCount).isEqualTo(successCount.get())
        }

        println("\n=== 🏆 대량 좌석 예약 경쟁 결과 ===")
        println("✅ 총 예약 성공: ${successCount.get()}")
        println("❌ 총 예약 실패: ${failureCount.get()}")
        println("🎯 예상 성공 수: ${seatIds.size}")

        println("\n🎫 좌석별 예약 결과:")
        seatIds.forEach { seatId ->
            val seat = concertSeatRepository.findByConcertSeatId(seatId)
            val winner = seatWinners[seatId] ?: "예약 실패"
            println("  좌석 $seatId: ${seat?.seatStatus} → $winner")
        }

        // 검증: 각 좌석마다 정확히 하나씩만 예약되어야 함
        assertThat(successCount.get()).isEqualTo(seatIds.size)
        assertThat(seatWinners).hasSize(seatIds.size)
        assertThat(failureCount.get()).isEqualTo(totalRequests - seatIds.size)
    }

    @Test
    @DisplayName("Awaitility - 혼합 시나리오: 포인트 + 좌석 예약 동시 실행")
    fun testMixedPointAndReservationConcurrency() {
        val userId = "user-7"
        val seatId = 2L
        val tokenId = "token-7"
        val pointOperations = 15
        val reservationAttempts = 8
        val totalOperations = pointOperations + reservationAttempts

        val pointSuccessCount = AtomicInteger(0)
        val reservationSuccessCount = AtomicInteger(0)
        val optimisticLockFailures = AtomicInteger(0)
        val completedOperations = AtomicInteger(0)

        println("=== 🎭 혼합 시나리오 테스트: 포인트 + 좌석 예약 ===")
        println("사용자: $userId, 좌석: $seatId")
        println("포인트 작업: $pointOperations, 예약 시도: $reservationAttempts")

        val initialUser = userRepository.findByUserId(userId)!!
        println("초기 포인트: ${initialUser.totalPoint}")

        // 포인트 충전/사용 작업들
        repeat(pointOperations) { index ->
            executorService.submit {
                try {
                    if (index % 2 == 0) {
                        chargeUserPointUseCase.chargeUserPoint(
                            ChargeUserPointCommand(userId, 500)
                        )
                        println("[POINT-$index] ✅ 충전 성공")
                    } else {
                        useUserPointUseCase.useUserPoint(
                            UseUserPointCommand(userId, 300)
                        )
                        println("[POINT-$index] ✅ 사용 성공")
                    }
                    pointSuccessCount.incrementAndGet()

                } catch (e: OptimisticLockingFailureException) {
                    optimisticLockFailures.incrementAndGet()
                    println("[POINT-$index] 🔒 낙관적 락 실패")

                } catch (e: Exception) {
                    println("[POINT-$index] ❌ 실패: ${e.message}")
                } finally {
                    completedOperations.incrementAndGet()
                }
            }
        }

        // 좌석 예약 시도들 (동일 좌석에 여러 번 시도)
        repeat(reservationAttempts) { index ->
            executorService.submit {
                try {
                    println("[SEAT-$index] 좌석 $seatId 예약 시도")

                    tempReservationUseCase.tempReservation(
                        TempReservationCommand(tokenId, userId, seatId)
                    )

                    reservationSuccessCount.incrementAndGet()
                    println("[SEAT-$index] ✅ 좌석 예약 성공")

                } catch (e: Exception) {
                    println("[SEAT-$index] ❌ 좌석 예약 실패")
                } finally {
                    completedOperations.incrementAndGet()
                }
            }
        }

        // 모든 작업 완료 대기
        await withPollInterval Duration.ofMillis(100) atMost Duration.ofSeconds(45) untilAsserted {
            assertThat(completedOperations.get()).isEqualTo(totalOperations)
        }

        val finalUser = userRepository.findByUserId(userId)!!
        val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)!!

        println("\n=== 🎯 혼합 시나리오 테스트 결과 ===")
        println("✅ 포인트 작업 성공: ${pointSuccessCount.get()}")
        println("✅ 좌석 예약 성공: ${reservationSuccessCount.get()}")
        println("🔒 낙관적 락 실패: ${optimisticLockFailures.get()}")
        println("💰 최종 사용자 포인트: ${finalUser.totalPoint}")
        println("🪑 최종 좌석 상태: ${finalSeat.seatStatus}")

        // 검증
        assertThat(pointSuccessCount.get()).isGreaterThan(0)
        assertThat(reservationSuccessCount.get()).isLessThanOrEqualTo(1) // 좌석은 최대 1번만 예약
        assertThat(optimisticLockFailures.get()).isGreaterThan(0) // User 엔터티의 @Version 충돌
        assertThat(finalUser.totalPoint).isGreaterThanOrEqualTo(0)
    }
}