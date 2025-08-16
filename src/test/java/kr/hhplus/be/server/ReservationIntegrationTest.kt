package kr.hhplus.be.server

import kotlinx.coroutines.*
import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.queue.GenerateQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.concert.*
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.config.TestRedisConfig
import kr.hhplus.be.server.domain.concert.*
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationIntegrationTest : IntegrationTestBase() {

    private val log = LoggerFactory.getLogger(ReservationIntegrationTest::class.java)

    @Autowired
    private lateinit var tempReservationUseCase: TempReservationUseCase

    @Autowired
    private lateinit var generateQueueTokenUseCase: GenerateQueueTokenUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertDateRepository: ConcertDateRepository

    @Autowired
    private lateinit var concertSeatRepository: ConcertSeatRepository

    @Autowired
    private lateinit var concertSeatGradeRepository: ConcertSeatGradeRepository

    @Autowired
    private lateinit var tempReservationRepository: TempReservationRepository

    @Autowired
    private lateinit var queueTokenRepository: QueueTokenRepository

    @Autowired(required = false)
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    private var testUserIds: List<String> = emptyList()
    private lateinit var testConcert: Concert
    private lateinit var testConcertDate: ConcertDate
    private lateinit var testSeats: List<ConcertSeat>
    private lateinit var testTokens: Map<String, String>

    @BeforeEach
    fun setUp() {
        // Redis 초기화
        try {
            redisTemplate?.let {
                val connection = it.connectionFactory?.connection
                connection?.serverCommands()?.flushAll()
                connection?.close()
                log.info("Redis flushed for test")
            }
        } catch (e: Exception) {
            log.warn("Failed to flush Redis: ${e.message}")
        }

        cleanupTestData()
        setupTestData()
    }

    @AfterEach
    fun tearDown() {
        cleanupTestData()
    }

    private fun setupTestData() {
        val timestamp = System.currentTimeMillis()

        // 사용자 생성
        testUserIds = (1..10).map { index ->
            val userId = "test-user-$timestamp-$index"

            val existingUser = userRepository.findByUserId(userId)
            if (existingUser == null) {
                val newUser = User(
                    userId = userId,
                    userName = "Test User $index",
                    totalPoint = 100000,
                    availablePoint = 100000,
                    usedPoint = 0,
                    version = 0
                )
                userRepository.save(newUser)
                log.info("Created user: $userId")
            }
            userId
        }

        // 콘서트 생성
        testConcert = concertRepository.save(
            Concert(
                concertId = 0L,
                concertName = "Test Concert $timestamp",
                location = "Test Location",
                description = "Test Description"
            )
        )
        log.info("Created concert: ${testConcert.concertId}")

        // 콘서트 날짜 생성
        testConcertDate = concertDateRepository.save(
            ConcertDate(
                concertDateId = 0L,
                concertSession = 1,
                concertId = testConcert.concertId,
                date = LocalDateTime.now().plusDays(7),
                isSoldOut = false
            )
        )
        log.info("Created concert date: ${testConcertDate.concertDateId}")

        // 좌석 등급 생성
        listOf("VIP", "STANDARD", "ECONOMY").forEach { grade ->
            concertSeatGradeRepository.save(
                ConcertSeatGrade(
                    concertSeatGradeId = 0L,
                    concertId = testConcert.concertId,
                    seatGrade = grade,
                    price = when(grade) {
                        "VIP" -> 150000
                        "STANDARD" -> 100000
                        else -> 50000
                    }
                )
            )
        }

        // 좌석 생성
        testSeats = (1..5).map { seatNum ->
            val seat = concertSeatRepository.save(
                ConcertSeat(
                    concertSeatId = 0L,
                    concertDateId = testConcertDate.concertDateId,
                    seatNumber = "A$seatNum",
                    seatGrade = "STANDARD",
                    seatStatus = SeatStatus.AVAILABLE
                )
            )
            log.info("Created seat: ${seat.concertSeatId} - ${seat.seatNumber}")
            seat
        }

        testTokens = testUserIds.associate { userId ->
            val result = generateQueueTokenUseCase.generateToken(
                GenerateQueueTokenCommand(
                    userId = userId,
                    concertId = testConcert.concertId
                )
            )

            val token = queueTokenRepository.findByTokenId(result.tokenId)
            if (token != null && token.tokenStatus == QueueTokenStatus.WAITING) {
                val activatedToken = token.copy(
                    tokenStatus = QueueTokenStatus.ACTIVE,
                    expiresAt = LocalDateTime.now().plusMinutes(30)
                )
                queueTokenRepository.save(activatedToken)
                log.info("Activated token for user $userId: ${result.tokenId}")
            }

            userId to result.tokenId
        }
    }

    private fun cleanupTestData() {
        try {
            tempReservationRepository.findAll()
                .filter { reservation ->
                    testUserIds.any { userId -> reservation.userId == userId }
                }
                .forEach { tempReservation ->
                    try {
                        tempReservationRepository.save(tempReservation.expire())
                    } catch (e: Exception) {
                        log.debug("Failed to expire reservation: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            log.debug("Cleanup failed: ${e.message}")
        }
    }

    @Test
    @DisplayName("동시에 같은 좌석 예약시 한명만 성공")
    fun testConcurrentSameSeatReservation() {
        runBlocking {
            // given
            val targetSeat = testSeats.first()
            val selectedUserIds = testUserIds.take(5)
            log.info("Testing concurrent reservation for seat: ${targetSeat.concertSeatId}")

            // when
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            val jobs = selectedUserIds.map { userId ->
                async(Dispatchers.IO) {
                    try {
                        val result = tempReservationUseCase.tempReservation(
                            TempReservationCommand(
                                tokenId = testTokens[userId]!!,
                                userId = userId,
                                concertSeatId = targetSeat.concertSeatId
                            )
                        )
                        successCount.incrementAndGet()
                        log.info("User $userId successfully reserved seat ${targetSeat.concertSeatId}")
                        true
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        log.info("User $userId failed to reserve seat: ${e.message}")
                        false
                    }
                }
            }

            jobs.awaitAll()

            // then
            log.info("Success: ${successCount.get()}, Fail: ${failCount.get()}")
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(failCount.get()).isEqualTo(4)

            val seat = concertSeatRepository.findByConcertSeatId(targetSeat.concertSeatId)
            assertThat(seat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
        }
    }

    @Test
    @DisplayName("다른 좌석 예약시 모두 성공")
    fun testDifferentSeatsReservation() {
        runBlocking {
            // given
            val selectedUserIds = testUserIds.take(5)
            log.info("Testing different seats reservation for ${selectedUserIds.size} users")

            // when
            val successCount = AtomicInteger(0)
            val jobs = selectedUserIds.mapIndexed { index, userId ->
                async(Dispatchers.IO) {
                    try {
                        val seatId = testSeats[index].concertSeatId
                        log.info("User $userId attempting to reserve seat $seatId")

                        val result = tempReservationUseCase.tempReservation(
                            TempReservationCommand(
                                tokenId = testTokens[userId]!!,
                                userId = userId,
                                concertSeatId = seatId
                            )
                        )
                        successCount.incrementAndGet()
                        log.info("User $userId successfully reserved seat $seatId")
                        true
                    } catch (e: Exception) {
                        log.error("User $userId failed to reserve seat: ${e.message}", e)
                        false
                    }
                }
            }

            val results = jobs.awaitAll()

            // then
            log.info("Total successful reservations: ${successCount.get()}")
            assertThat(successCount.get()).isEqualTo(5)

            testSeats.take(5).forEach { seat ->
                val updatedSeat = concertSeatRepository.findByConcertSeatId(seat.concertSeatId)
                assertThat(updatedSeat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }
    }

    @Test
    @DisplayName("대량 동시 예약시 좌석수만큼만 성공")
    fun testMassiveConcurrentReservations() {
        runBlocking {
            // given
            val totalSeats = testSeats.size
            log.info("Testing massive concurrent reservations for $totalSeats seats")

            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val reservedSeats = mutableSetOf<Long>()

            // when
            val jobsBySeats = testSeats.map { seat ->
                val seatJobs = testUserIds.take(3).map { userId ->
                    async(Dispatchers.IO) {
                        delay((0..50).random().toLong()) // 약간의 랜덤 지연으로 동시성 상황 만들기
                        try {
                            val result = tempReservationUseCase.tempReservation(
                                TempReservationCommand(
                                    tokenId = testTokens[userId]!!,
                                    userId = userId,
                                    concertSeatId = seat.concertSeatId
                                )
                            )
                            synchronized(reservedSeats) {
                                reservedSeats.add(seat.concertSeatId)
                            }
                            val count = successCount.incrementAndGet()
                            log.info("Success #$count: User $userId reserved seat ${seat.seatNumber} (${seat.concertSeatId})")
                            true
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                            log.debug("User $userId failed to reserve seat ${seat.seatNumber}: ${e.message}")
                            false
                        }
                    }
                }
                seatJobs
            }.flatten()

            val results = jobsBySeats.awaitAll()

            // then
            val actualSuccessCount = successCount.get()
            val actualFailCount = failCount.get()

            log.info("Final results - Success: $actualSuccessCount, Fail: $actualFailCount")
            log.info("Reserved seats: ${reservedSeats.size}, Total seats: $totalSeats")
            log.info("Reserved seat IDs: $reservedSeats")

            // 각 좌석당 정확히 1명만 성공해야 함
            assertThat(reservedSeats.size).isEqualTo(totalSeats)
            assertThat(actualSuccessCount).isEqualTo(totalSeats)

            // 모든 좌석이 예약되었는지 확인
            testSeats.forEach { seat ->
                val updatedSeat = concertSeatRepository.findByConcertSeatId(seat.concertSeatId)
                log.info("Seat ${seat.seatNumber} (${seat.concertSeatId}) status: ${updatedSeat?.seatStatus}")
                assertThat(updatedSeat?.seatStatus)
                    .withFailMessage("Seat ${seat.seatNumber} should be RESERVED but was ${updatedSeat?.seatStatus}")
                    .isEqualTo(SeatStatus.RESERVED)
            }
        }
    }
}