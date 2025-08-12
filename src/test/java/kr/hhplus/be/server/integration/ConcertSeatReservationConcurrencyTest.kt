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
@DisplayName("콘서트 좌석 예약 1대1 경합 테스트")
class ConcertSeatReservationConcurrencyTest {

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
    }

    private fun cleanupTestData() {
    }

    private fun createTestUser(userId: String): User {
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


    private fun createTestSeat(seatId: Long, seatNumber: String = "A1"): ConcertSeat {
        val seat = ConcertSeat(
            concertSeatId = seatId,
            concertDateId = 100L,
            seatNumber = seatNumber,
            seatGrade = "VIP",
            seatStatus = SeatStatus.AVAILABLE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    @DisplayName("1대1 좌석 예약 경합 테스트 - NOWAIT 락")
    fun `one_vs_one_seat_reservation_battle`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val seatId = timestamp // 고유한 좌석 ID

        val user1Id = "fighter-1-$timestamp"
        val user2Id = "fighter-2-$timestamp"
        val token1Id = "token-1-$timestamp"
        val token2Id = "token-2-$timestamp"

        // 테스트 데이터 생성
        createTestUser(user1Id)
        createTestUser(user2Id)
        createTestQueueToken(token1Id, user1Id)
        createTestQueueToken(token2Id, user2Id)
        val testSeat = createTestSeat(seatId, "VIP-1")

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val lockFailureCount = AtomicInteger(0)
        val winner = ConcurrentLinkedQueue<String>()
        val loser = ConcurrentLinkedQueue<String>()

        println("🥊 === 1대1 좌석 예약 경합 시작! ===")
        println("좌석: ${testSeat.seatNumber} (ID: $seatId)")
        println("🔴 도전자1: $user1Id")
        println("🔵 도전자2: $user2Id")
        println("초기 좌석 상태: ${testSeat.seatStatus}")

        val startTime = System.currentTimeMillis()

        // When - 동시에 같은 좌석 예약 시도
        val fighter1 = async(Dispatchers.IO) {
            try {
                // 약간의 랜덤 지연으로 더 현실적인 시나리오 구현
                delay(Random.nextLong(1, 10))

                tempReservationUseCase.tempReservation(
                    TempReservationCommand(token1Id, user1Id, seatId)
                )

                successCount.incrementAndGet()
                winner.add(user1Id)
                println("🏆 [도전자1] $user1Id 승리! 예약 성공!")

            } catch (e: PessimisticLockingFailureException) {
                lockFailureCount.incrementAndGet()
                loser.add(user1Id)
                println("💀 [도전자1] $user1Id 패배 - 락 경합 실패")

            } catch (e: IllegalStateException) {
                failureCount.incrementAndGet()
                loser.add(user1Id)
                println("💀 [도전자1] $user1Id 패배 - 좌석 이미 예약됨")

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
                println("🏆 [도전자2] $user2Id 승리! 예약 성공!")

            } catch (e: PessimisticLockingFailureException) {
                lockFailureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - 락 경합 실패")

            } catch (e: IllegalStateException) {
                failureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - 좌석 이미 예약됨")

            } catch (e: Exception) {
                failureCount.incrementAndGet()
                loser.add(user2Id)
                println("💀 [도전자2] $user2Id 패배 - ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        // 두 도전자의 경합 결과 대기
        fighter1.await()
        fighter2.await()

        val endTime = System.currentTimeMillis()
        val battleDuration = endTime - startTime

        // Then - 경합 결과 검증
        await withPollInterval Duration.ofMillis(50) untilAsserted {
            val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)!!

            if (successCount.get() == 1) {
                assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }

        val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)!!
        val championUser = winner.firstOrNull()
        val defeatedUser = loser.firstOrNull()

        println("\n🎪 === 경합 결과 발표! ===")
        println("⚔️  경합 시간: ${battleDuration}ms")
        println("🏆 승자: ${championUser ?: "무승부"}")
        println("💀 패자: ${defeatedUser ?: "무승부"}")
        println("✅ 예약 성공: ${successCount.get()}명")
        println("🔒 락 실패: ${lockFailureCount.get()}명")
        println("❌ 기타 실패: ${failureCount.get()}명")
        println("🪑 최종 좌석 상태: ${finalSeat.seatStatus}")
        println("⚡ 즉시 처리 여부: ${if (battleDuration < 100) "✅ 즉시 처리" else "⚠️ 지연 발생"}")

        // 핵심 검증 - 1대1 경합의 핵심 룰
        assertThat(successCount.get()).isEqualTo(1) // 정확히 1명만 승리
        assertThat(lockFailureCount.get() + failureCount.get()).isEqualTo(1) // 정확히 1명만 패배
        assertThat(winner).hasSize(1) // 승자는 1명
        assertThat(loser).hasSize(1) // 패자는 1명
        assertThat(finalSeat.seatStatus).isEqualTo(SeatStatus.RESERVED) // 좌석은 예약됨
        assertThat(battleDuration).isLessThan(500) // 0.5초 이내 경합 완료
    }

    @Test
    @DisplayName("다중 좌석 동시 예약 경합 테스트 - 각자 다른 좌석")
    fun `multiple_seats_concurrent_reservation_test`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val seatCount = 5
        val usersPerSeat = 3 // 각 좌석마다 3명이 경합

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val seatWinners = ConcurrentLinkedQueue<Pair<Long, String>>()

        println("🏟️ === 다중 좌석 동시 예약 경합! ===")
        println("좌석 수: $seatCount")
        println("좌석당 경합자: $usersPerSeat 명")

        // 테스트 데이터 생성
        val testSeats = (1..seatCount).map { seatIndex ->
            createTestSeat(timestamp + seatIndex, "A$seatIndex")
        }

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

                        delay(Random.nextLong(1, 20))

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
                val finalSeat = concertSeatRepository.findByConcertSeatId(seat.concertSeatId)!!
                finalSeat.seatStatus == SeatStatus.RESERVED
            }
            assertThat(reservedSeats).isEqualTo(seatCount)
        }

        println("\n🎊 === 다중 좌석 경합 결과 ===")
        println("⏱️  총 소요시간: ${endTime - startTime}ms")
        println("🏆 총 예약 성공: ${successCount.get()}/${seatCount}석")
        println("💀 총 예약 실패: ${failureCount.get()}명")
        println("📊 예약 성공률: ${String.format("%.1f", successCount.get() * 100.0 / seatCount)}%")

        println("\n🎯 좌석별 승자:")
        seatWinners.forEach { (seatId, winnerId) ->
            val seat = testSeats.find { it.concertSeatId == seatId }
            println("  ${seat?.seatNumber}: $winnerId")
        }

        // 검증
        assertThat(successCount.get()).isEqualTo(seatCount) // 모든 좌석이 예약되어야 함
        assertThat(seatWinners).hasSize(seatCount) // 승자도 좌석 수와 동일
        assertThat(failureCount.get()).isEqualTo((usersPerSeat - 1) * seatCount) // 나머지는 모두 실패
    }

    @Test
    @DisplayName("황금 좌석 쟁탈전 - 10명이 1개 좌석 경합")
    fun `golden_seat_battle_royale_test`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val seatId = timestamp
        val fighters = 10

        val successCount = AtomicInteger(0)
        val lockFailures = AtomicInteger(0)
        val otherFailures = AtomicInteger(0)
        val battleResults = ConcurrentLinkedQueue<Pair<String, String>>() // userId to result

        println("👑 === 황금 좌석 쟁탈전! ===")
        println("🎯 황금 좌석: GOLD-$seatId")
        println("⚔️  참가자: $fighters 명")

        // 황금 좌석 생성
        createTestSeat(seatId, "GOLD-SEAT")

        val battleStart = System.currentTimeMillis()

        // When - 10명이 동시에 황금 좌석 쟁탈
        val battleJobs = (1..fighters).map { fighterNum ->
            async(Dispatchers.IO) {
                val fighterId = "fighter-$fighterNum-$timestamp"
                val tokenId = "token-$fighterNum-$timestamp"

                try {
                    // 전사 등록
                    createTestUser(fighterId)

                    // 전투 시작! (약간의 랜덤 딜레이로 현실적 시뮬레이션)
                    delay(Random.nextLong(1, 50))

                    println("⚔️ [$fighterId] 황금 좌석 돌진!")

                    tempReservationUseCase.tempReservation(
                        TempReservationCommand(tokenId, fighterId, seatId)
                    )

                    successCount.incrementAndGet()
                    battleResults.add(fighterId to "🏆 VICTORY")
                    println("👑 [$fighterId] 황금 좌석 획득! 전설이 되었다!")

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
            val finalSeat = concertSeatRepository.findByConcertSeatId(seatId)!!
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
        println("📊 생존율: ${String.format("%.1f", successCount.get() * 100.0 / fighters)}%")

        println("\n⚰️ 전사들의 최후:")
        battleResults.forEach { (fighter, result) ->
            println("  $fighter: $result")
        }

        // 최종 검증 - 배틀 로얄의 철칙
        assertThat(successCount.get()).isEqualTo(1) // 오직 1명만 살아남는다
        assertThat(lockFailures.get() + otherFailures.get()).isEqualTo(fighters - 1) // 나머지는 모두 패배
        assertThat(champion).isNotNull() // 반드시 승자가 있어야 함
        assertThat(battleDuration).isLessThan(2000) // 2초 이내 전투 종료
    }
}