package kr.hhplus.be.server

import kotlinx.coroutines.*
import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.reservation.TempReservationCommand
import kr.hhplus.be.server.application.port.`in`.queue.GenerateQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.reservation.TempReservationUseCase
import kr.hhplus.be.server.application.port.out.concert.*
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.concert.*
import kr.hhplus.be.server.domain.reservation.TempReservationStatus
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationIntegrationTest : IntegrationTestBase() {

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

    private lateinit var testUsers: List<User>
    private lateinit var testConcert: Concert
    private lateinit var testConcertDate: ConcertDate
    private lateinit var testSeats: List<ConcertSeat>
    private lateinit var testTokens: Map<String, String>

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        setupTestData()
    }

    @AfterEach
    fun tearDown() {
        cleanupTestData()
    }

    private fun setupTestData() {
        // 사용자 생성
        testUsers = (1..10).map { index ->
            userRepository.save(
                User(
                    userId = "test-user-$index",
                    userName = "Test User $index",
                    totalPoint = 100000,
                    availablePoint = 100000,
                    usedPoint = 0
                )
            )
        }

        // 콘서트 생성
        testConcert = concertRepository.save(
            Concert(
                concertId = 0L,
                concertName = "Test Concert",
                location = "Test Location",
                description = "Test Description"
            )
        )

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

        // 좌석 생성 (5개만 생성하여 경합 상황 유도)
        testSeats = (1..5).map { seatNum ->
            concertSeatRepository.save(
                ConcertSeat(
                    concertSeatId = 0L,
                    concertDateId = testConcertDate.concertDateId,
                    seatNumber = "A$seatNum",
                    seatGrade = "STANDARD",
                    seatStatus = SeatStatus.AVAILABLE
                )
            )
        }

        // 각 사용자에 대한 활성 토큰 생성
        testTokens = testUsers.associate { user ->
            val result = generateQueueTokenUseCase.generateToken(
                GenerateQueueTokenCommand(
                    userId = user.userId,
                    concertId = testConcert.concertId
                )
            )
            user.userId to result.tokenId
        }
    }

    private fun cleanupTestData() {
        tempReservationRepository.findAll()
            .filter { it.userId.startsWith("test-user-") }
            .forEach { tempReservationRepository.save(it.expire()) }
    }

    @Test
    @DisplayName("동시에 같은 좌석 예약시 한명만 성공")
    fun testConcurrentSameSeatReservation() {
        runBlocking {
            // given
            val targetSeat = testSeats.first()

            // when
            val jobs = testUsers.take(5).map { user ->
                async(Dispatchers.IO) {
                    try {
                        tempReservationUseCase.tempReservation(
                            TempReservationCommand(
                                tokenId = testTokens[user.userId]!!,
                                userId = user.userId,
                                concertSeatId = targetSeat.concertSeatId
                            )
                        )
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            val results = jobs.awaitAll()

            // then
            val successCount = results.count { it }
            val failCount = results.count { !it }

            assertThat(successCount).isEqualTo(1)
            assertThat(failCount).isEqualTo(4)

            val seat = concertSeatRepository.findByConcertSeatId(targetSeat.concertSeatId)
            assertThat(seat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
        }
    }

    @Test
    @DisplayName("다른 좌석 예약시 모두 성공")
    fun testDifferentSeatsReservation() {
        runBlocking {
            // given
            val users = testUsers.take(5)

            // when
            val jobs = users.mapIndexed { index, user ->
                async(Dispatchers.IO) {
                    try {
                        tempReservationUseCase.tempReservation(
                            TempReservationCommand(
                                tokenId = testTokens[user.userId]!!,
                                userId = user.userId,
                                concertSeatId = testSeats[index].concertSeatId
                            )
                        )
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            val results = jobs.awaitAll()

            // then
            val successCount = results.count { it }
            assertThat(successCount).isEqualTo(5)

            testSeats.take(5).forEach { seat ->
                val updatedSeat = concertSeatRepository.findByConcertSeatId(seat.concertSeatId)
                assertThat(updatedSeat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }
    }

    @Test
    @DisplayName("한 사용자가 여러 좌석 동시 예약")
    fun testSameUserMultipleSeats() {
        runBlocking {
            // given
            val user = testUsers.first()
            val tokenId = testTokens[user.userId]!!
            val seatsToReserve = testSeats.take(3)

            // when
            val jobs = seatsToReserve.map { seat ->
                async(Dispatchers.IO) {
                    try {
                        tempReservationUseCase.tempReservation(
                            TempReservationCommand(
                                tokenId = tokenId,
                                userId = user.userId,
                                concertSeatId = seat.concertSeatId
                            )
                        )
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            val results = jobs.awaitAll()

            // then
            val successCount = results.count { it }
            assertThat(successCount).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("이미 예약된 좌석 재예약 실패")
    fun testAlreadyReservedSeat() {
        runBlocking {
            // given
            val targetSeat = testSeats.first()
            val firstUser = testUsers.first()
            val secondUser = testUsers[1]

            val firstReservation = tempReservationUseCase.tempReservation(
                TempReservationCommand(
                    tokenId = testTokens[firstUser.userId]!!,
                    userId = firstUser.userId,
                    concertSeatId = targetSeat.concertSeatId
                )
            )

            assertThat(firstReservation.status).isEqualTo(TempReservationStatus.RESERVED)

            // when & then
            val exception = assertThrows<Exception> {
                tempReservationUseCase.tempReservation(
                    TempReservationCommand(
                        tokenId = testTokens[secondUser.userId]!!,
                        userId = secondUser.userId,
                        concertSeatId = targetSeat.concertSeatId
                    )
                )
            }

            assertThat(exception.message).contains("already")
        }
    }

    @Test
    @DisplayName("대량 동시 예약시 좌석수만큼만 성공")
    fun testMassiveConcurrentReservations() {
        runBlocking {
            // given
            val totalSeats = testSeats.size
            val latch = CountDownLatch(1)

            // when
            val jobs = testUsers.flatMap { user ->
                testSeats.map { seat ->
                    async(Dispatchers.IO) {
                        latch.await()
                        try {
                            tempReservationUseCase.tempReservation(
                                TempReservationCommand(
                                    tokenId = testTokens[user.userId]!!,
                                    userId = user.userId,
                                    concertSeatId = seat.concertSeatId
                                )
                            )
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }

            latch.countDown()
            val results = jobs.awaitAll()

            // then
            val successCount = results.count { it }
            assertThat(successCount).isEqualTo(totalSeats)

            testSeats.forEach { seat ->
                val updatedSeat = concertSeatRepository.findByConcertSeatId(seat.concertSeatId)
                assertThat(updatedSeat?.seatStatus).isEqualTo(SeatStatus.RESERVED)
            }
        }
    }
}