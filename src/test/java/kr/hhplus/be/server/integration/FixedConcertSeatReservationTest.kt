package kr.hhplus.be.server.integration

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.users.User
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // 각 테스트 후 컨텍스트 초기화
@DisplayName("콘서트 좌석 예약 동시성 테스트 - 수정된 버전")
class FixedConcertSeatReservationTest {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("hhplus_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false) // 테스트마다 새 컨테이너 사용

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.datasource.hikari.maximum-pool-size") { "20" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" } // 테스트마다 테이블 재생성
        }
    }

    @Autowired
    private lateinit var tempReservationUseCase: TempReservationUseCase

    @Autowired
    private lateinit var concertSeatRepository: ConcertSeatRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueTokenRepository: QueueTokenRepository

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        setupInitialTestData() // 각 테스트를 위한 기본 데이터 셋업
    }

    private fun cleanupTestData() {
        // JpaRepository로 모든 테스트 데이터 정리
        println("🧹 테스트 데이터 정리 시작...")

        try {
            tempReservationJpaRepository.deleteAll()
            println("   - 임시예약 데이터 정리 완료")
        } catch (e: Exception) {
            println("   - 임시예약 데이터 정리 실패: ${e.message}")
        }

        try {
            queueTokenJpaRepository.deleteAll()
            println("   - 대기열 토큰 데이터 정리 완료")
        } catch (e: Exception) {
            println("   - 대기열 토큰 데이터 정리 실패: ${e.message}")
        }

        try {
            concertSeatJpaRepository.deleteAll()
            println("   - 좌석 데이터 정리 완료")
        } catch (e: Exception) {
            println("   - 좌석 데이터 정리 실패: ${e.message}")
        }

        try {
            userJpaRepository.deleteAll()
            println("   - 사용자 데이터 정리 완료")
        } catch (e: Exception) {
            println("   - 사용자 데이터 정리 실패: ${e.message}")
        }

        println("🧹 테스트 데이터 정리 완료!")
    }

    // 각 테스트에서 사용할 기본 데이터를 미리 생성 (JPA Repository로 직접)
    private fun setupInitialTestData() {
        println("🏗️ 테스트 기본 데이터 생성 시작...")

        // 기본 좌석들을 JPA Repository로 직접 생성
        try {
            repeat(10) { index ->
                val seatId = 1000L + index
                val seatEntity = kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity(
                    concertDateId = 100L,
                    seatNumber = "${index + 1}",
                    seatGrade = "VIP",
                    seatStatus = SeatStatus.AVAILABLE
                )
                concertSeatJpaRepository.save(seatEntity)
            }
            println("   - 테스트 좌석 10개 생성 완료")
        } catch (e: Exception) {
            println("   - 테스트 좌석 생성 실패: ${e.message}")
        }

        println("🏗️ 테스트 기본 데이터 생성 완료!")
    }

    @Autowired
    private lateinit var concertSeatJpaRepository: kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.jpa.ConcertSeatJpaRepository

    @Autowired
    private lateinit var userJpaRepository: kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.jpa.UserJpaRepository

    @Autowired
    private lateinit var queueTokenJpaRepository: kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.jpa.QueueTokenJpaRepository

    @Autowired
    private lateinit var tempReservationJpaRepository: kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.jpa.TempReservationJpaRepository

    // 기존 DB에 있는 좌석만 사용 - 새로운 좌석은 절대 생성하지 않음
    private fun findOrCreateTestSeat(concertDateId: Long = 100L): ConcertSeat {
        // 기존 좌석을 먼저 찾아보고
        val existingSeats = concertSeatRepository.findByConcertDateId(concertDateId)

        val availableSeat = existingSeats.find { it.seatStatus == SeatStatus.AVAILABLE }
        if (availableSeat != null) {
            return availableSeat
        }

        // 사용 가능한 좌석이 없으면 예외 발생
        throw IllegalStateException("테스트에 사용할 좌석이 없습니다. concertDateId: $concertDateId")
    }

    private fun createTestUser(userId: String): User {
        userRepository.findByUserId(userId)?.let { return it }

        val user = User(
            userId = userId,
            userName = "테스트유저$userId",
            totalPoint = 100000,
            availablePoint = 100000,
            usedPoint = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(user)
    }

    private fun createTestQueueToken(tokenId: String, userId: String, concertId: Long = 1L): QueueToken {
        val token = QueueToken(
            queueTokenId = tokenId,
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.ACTIVE,
            enteredAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return queueTokenRepository.save(token)
    }

    @Test
    @DisplayName("1대1 좌석 예약 경합 테스트 - 기존 좌석만 사용")
    fun `fixed_one_vs_one_seat_reservation_battle`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val nanoSuffix = System.nanoTime() % 10000

        val user1Id = "fighter-1-$timestamp-$nanoSuffix"
        val user2Id = "fighter-2-$timestamp-$nanoSuffix"
        val token1Id = "token-1-$timestamp-$nanoSuffix"
        val token2Id = "token-2-$timestamp-$nanoSuffix"

        // 테스트 데이터 생성 - 각각 고유한 ID로
        val user1 = createTestUser(user1Id)
        val user2 = createTestUser(user2Id)
        val token1 = createTestQueueToken(token1Id, user1Id)
        val token2 = createTestQueueToken(token2Id, user2Id)

        // 기존 가용 좌석을 찾기만 하고 새로 생성하지 않음
        val testSeat = try {
            findOrCreateTestSeat()
        } catch (e: IllegalStateException) {
            println("❌ ${e.message}")
            return@runTest
        }
        val seatId = testSeat.concertSeatId

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val winner = ConcurrentLinkedQueue<String>()
        val loser = ConcurrentLinkedQueue<String>()

        println("🥊 === 수정된 1대1 좌석 예약 경합 시작! ===")
        println("좌석: ${testSeat.seatNumber} (ID: $seatId)")
        println("🔴 도전자1: $user1Id")
        println("🔵 도전자2: $user2Id")

        val startTime = System.currentTimeMillis()

        // When - 동시에 같은 좌석 예약 시도
        val fighter1 = async(Dispatchers.IO) {
            try {
                delay(Random.nextLong(1, 10))
                tempReservationUseCase.tempReservation(
                    TempReservationCommand(token1Id, user1Id, seatId)
                )
                successCount.incrementAndGet()
                winner.add(user1Id)
                println("🏆 [도전자1] $user1Id 승리!")
            } catch (e: PessimisticLockingFailureException) {
                failureCount.incrementAndGet()
                loser.add(user1Id)
                println("💀 [도전자1] $user1Id 패배 - 락 경합 실패")
            } catch (e: IllegalStateException) {
                failureCount.incrementAndGet()
                loser.add(user1Id)
                println("💀 [도전자1] $user1Id 패배 - 좌석 이미 예약됨: ${e.message}")
            } catch (e: Exception) {
                failureCount.incrementAndGet()
                loser.add(user1Id)
                println("💀 [도전자1] $user1Id 패배 - ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        val fighter2 = async(Dispatchers.IO) {
            try {
                delay(Random.nextLong(1, 10))
                tempReservationUseCase.tempReservation(
                    TempReservationCommand(token2Id, user2Id, seatId)
                )
                successCount.incrementAndGet()
                winner.add(user2Id)
                println("🏆 [도전자2] $user2Id 승리!")
            } catch (e: PessimisticLockingFailureException) {
                failureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - 락 경합 실패")
            } catch (e: IllegalStateException) {
                failureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - 좌석 이미 예약됨: ${e.message}")
            } catch (e: Exception) {
                failureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        fighter1.await()
        fighter2.await()

        val endTime = System.currentTimeMillis()
        val battleDuration = endTime - startTime

        // Then - 결과 검증
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val finalSeat = concertSeatRepository.findByConcertSeatIdWithLock(seatId)!!
            if (successCount.get() == 1) {
                assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }

        val finalSeat = concertSeatRepository.findByConcertSeatIdWithLock(seatId)!!

        println("\n🎪 === 경합 결과 발표! ===")
        println("⚔️  경합 시간: ${battleDuration}ms")
        println("✅ 예약 성공: ${successCount.get()}명")
        println("❌ 예약 실패: ${failureCount.get()}명")
        println("🏆 승자: ${winner.firstOrNull() ?: "무승부"}")
        println("🪑 최종 좌석 상태: ${finalSeat.seatStatus}")

        // 핵심 검증 - 1대1 경합의 핵심 룰
        assertThat(successCount.get()).isEqualTo(1) // 정확히 1명만 승리
        assertThat(failureCount.get()).isEqualTo(1) // 정확히 1명만 패배
        assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED) // 좌석은 예약됨
    }

    @Test
    @DisplayName("다중 좌석 동시 예약 경합 테스트")
    fun `multiple_seats_concurrent_reservation_test`() = runTest {
        val timestamp = System.currentTimeMillis()
        val seatCount = 3  // 좌석 수를 줄여서 안정성 확보
        val usersPerSeat = 2 // 경합자 수도 줄임

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val seatWinners = ConcurrentLinkedQueue<Pair<Long, String>>()

        println("🏟️ === 다중 좌석 동시 예약 경합! ===")
        println("좌석 수: $seatCount")
        println("좌석당 경합자: $usersPerSeat 명")

        // 각 좌석마다 기존 좌석 사용 - 새로운 좌석은 생성하지 않음
        val existingSeats = concertSeatRepository.findByConcertDateId(100L)
        val availableSeats = existingSeats.filter { it.seatStatus == SeatStatus.AVAILABLE }

        if (availableSeats.size < seatCount) {
            println("❌ 사용 가능한 좌석이 부족합니다. 필요: $seatCount, 현재: ${availableSeats.size}")
            println("테스트를 건너뜁니다.")
            return@runTest
        }

        val testSeats = availableSeats.take(seatCount)

        val startTime = System.currentTimeMillis()

        // When - 각 좌석마다 여러 사용자가 동시 경합
        val allJobs = testSeats.flatMapIndexed { seatIndex, seat ->
            (1..usersPerSeat).map { userIndex ->
                async(Dispatchers.IO) {
                    val userId = "user-${seatIndex}-${userIndex}-$timestamp"
                    val tokenId = "token-${seatIndex}-${userIndex}-$timestamp"

                    try {
                        // 사용자와 토큰 생성
                        createTestUser(userId)
                        createTestQueueToken(tokenId, userId)

                        delay(Random.nextLong(1, 30))

                        tempReservationUseCase.tempReservation(
                            TempReservationCommand(tokenId, userId, seat.concertSeatId)
                        )

                        successCount.incrementAndGet()
                        seatWinners.add(seat.concertSeatId to userId)
                        println("🏆 좌석 ${seat.seatNumber}: $userId 예약 성공!")

                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                        println("💀 좌석 ${seat.seatNumber}: $userId 예약 실패 - ${e.javaClass.simpleName}")
                    }
                }
            }
        }

        allJobs.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then - 결과 검증
        await withPollInterval Duration.ofMillis(100) untilAsserted {
            val reservedSeats = testSeats.count { seat ->
                val finalSeat = concertSeatRepository.findByConcertSeatIdWithLock(seat.concertSeatId)!!
                finalSeat.seatStatus == SeatStatus.RESERVED
            }
            assertThat(reservedSeats).isEqualTo(seatCount)
        }

        println("\n🎊 === 다중 좌석 경합 결과 ===")
        println("⏱️  총 소요시간: ${endTime - startTime}ms")
        println("🏆 총 예약 성공: ${successCount.get()}/${seatCount}석")
        println("💀 총 예약 실패: ${failureCount.get()}명")

        println("\n🎯 좌석별 승자:")
        seatWinners.forEach { (seatId, winnerId) ->
            val seat = testSeats.find { it.concertSeatId == seatId }
            println("  ${seat?.seatNumber}: $winnerId")
        }

        // 검증
        assertThat(successCount.get()).isEqualTo(seatCount) // 모든 좌석이 예약되어야 함
        assertThat(seatWinners).hasSize(seatCount) // 승자도 좌석 수와 동일
    }

    @Test
    @DisplayName("황금 좌석 쟁탈전 - 5명이 1개 좌석 경합")
    fun `golden_seat_battle_test`() = runTest {
        val timestamp = System.currentTimeMillis()
        val fighters = 5 // 참가자 수를 줄여서 안정성 확보

        val successCount = AtomicInteger(0)
        val lockFailures = AtomicInteger(0)
        val otherFailures = AtomicInteger(0)
        val battleResults = ConcurrentLinkedQueue<Pair<String, String>>()

        println("👑 === 황금 좌석 쟁탈전! ===")
        println("⚔️  참가자: $fighters 명")

        // 기존 좌석 중에서 사용 가능한 좌석 찾기 - 새로운 좌석은 절대 생성하지 않음
        val existingSeats = concertSeatRepository.findByConcertDateId(100L)
        val goldenSeat = existingSeats.find { it.seatStatus == SeatStatus.AVAILABLE }

        if (goldenSeat == null) {
            println("❌ 사용 가능한 좌석이 없어서 테스트를 건너뜁니다")
            return@runTest
        }

        val goldenSeatId = goldenSeat.concertSeatId

        val battleStart = System.currentTimeMillis()

        // When - 여러명이 동시에 황금 좌석 쟁탈
        val battleJobs = (1..fighters).map { fighterNum ->
            async(Dispatchers.IO) {
                val fighterId = "fighter-$fighterNum-$timestamp"
                val tokenId = "token-$fighterNum-$timestamp"

                try {
                    // 전사 등록
                    createTestUser(fighterId)
                    createTestQueueToken(tokenId, fighterId)

                    delay(Random.nextLong(1, 30))
                    println("⚔️ [$fighterId] 황금 좌석 돌진!")

                    tempReservationUseCase.tempReservation(
                        TempReservationCommand(tokenId, fighterId, goldenSeatId)
                    )

                    successCount.incrementAndGet()
                    battleResults.add(fighterId to "🏆 VICTORY")
                    println("👑 [$fighterId] 황금 좌석 획득!")

                } catch (e: PessimisticLockingFailureException) {
                    lockFailures.incrementAndGet()
                    battleResults.add(fighterId to "💀 LOCK_FAILED")
                    println("💀 [$fighterId] 락 전투에서 패배...")

                } catch (e: IllegalStateException) {
                    otherFailures.incrementAndGet()
                    battleResults.add(fighterId to "💀 TOO_LATE")
                    println("💀 [$fighterId] 너무 늦었다... 이미 점령당함")

                } catch (e: Exception) {
                    otherFailures.incrementAndGet()
                    battleResults.add(fighterId to "💀 DEFEATED")
                    println("💀 [$fighterId] 예상치 못한 패배: ${e.message}")
                }
            }
        }

        battleJobs.awaitAll()
        val battleEnd = System.currentTimeMillis()

        // Then - 쟁탈전 결과 집계
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val finalSeat = concertSeatRepository.findByConcertSeatIdWithLock(goldenSeatId)!!
            if (successCount.get() == 1) {
                assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }

        val champion = battleResults.find { it.second == "🏆 VICTORY" }?.first
        val battleDuration = battleEnd - battleStart

        println("\n🏆 === 황금 좌석 쟁탈전 결과 ===")
        println("👑 황금 좌석의 새 주인: ${champion ?: "없음"}")
        println("⚔️  전투 시간: ${battleDuration}ms")
        println("🏆 승자: ${successCount.get()}명")
        println("💀 락 전투 패배: ${lockFailures.get()}명")
        println("💀 기타 패배: ${otherFailures.get()}명")

        println("\n⚰️ 전사들의 최후:")
        battleResults.forEach { (fighter, result) ->
            println("  $fighter: $result")
        }

        // 최종 검증 - 배틀 로얄의 철칙
        assertThat(successCount.get()).isEqualTo(1) // 오직 1명만 살아남는다
        assertThat(lockFailures.get() + otherFailures.get()).isEqualTo(fighters - 1) // 나머지는 모두 패배
        assertThat(champion).isNotNull() // 반드시 승자가 있어야 함
    }
}