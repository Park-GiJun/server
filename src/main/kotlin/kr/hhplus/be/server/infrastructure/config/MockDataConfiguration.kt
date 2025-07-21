package kr.hhplus.be.server.infrastructure.config

import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.MockQueueTokenRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.MockUserRepository
import kr.hhplus.be.server.repository.mock.MockConcertDateRepository
import kr.hhplus.be.server.repository.mock.MockConcertRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatGradeRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class MockDataConfiguration {

    @Bean
    fun initMockData(
        mockQueueTokenRepository: MockQueueTokenRepository,
        mockUserRepository: MockUserRepository,
        mockConcertRepository: MockConcertRepository,
        mockConcertSeatGradeRepository: MockConcertSeatGradeRepository,
        mockConcertDateRepository: MockConcertDateRepository,
        mockConcertSeatRepository: MockConcertSeatRepository,
    ): CommandLineRunner {
        return CommandLineRunner { args ->
            val users = setUpInitialUsers(mockUserRepository)
            val concerts = setUpInitialConcert(mockConcertRepository)
            setUpInitialConcertSeatGrade(mockConcertSeatGradeRepository, concerts)
            val concertDates = setUpInitialConcertDates(mockConcertDateRepository, concerts)
            setUpInitialConcertSeats(mockConcertSeatRepository, concertDates)
        }
    }

    private fun setUpInitialUsers(repository: MockUserRepository): List<User> {
        val users = (1..100).map { index ->
            val totalPoint = (1000..10000).random()
            val usedPoint = (0..totalPoint/2).random()
            val availablePoint = totalPoint - usedPoint

            User(
                userId = "user-$index",
                userName = "user-$index",
                totalPoint = totalPoint,
                availablePoint = availablePoint,
                usedPoint = usedPoint
            )
        }

        users.forEach { user -> repository.save(user) }
        return users
    }

    private fun setUpInitialConcert(repository: MockConcertRepository): List<Concert> {
        val concerts = (1..2).map { index ->
            Concert(
                concertId = index.toLong(),
                concertName = "concert $index",
                location = "Test location $index"
            )
        }

        concerts.forEach { concert -> repository.save(concert) }
        return concerts
    }

    private fun setUpInitialConcertSeatGrade(repository: MockConcertSeatGradeRepository, concerts: List<Concert>) {
        val seatGrades = listOf(
            Triple("STANDING", 100000, "스탠딩"),
            Triple("VIP", 170000, "VIP석"),
            Triple("COMMON", 120000, "일반석")
        )

        var gradeId = 1L
        val concertSeatGrades = concerts.flatMap { concert ->
            seatGrades.map { (grade, price, _) ->
                ConcertSeatGrade(
                    concertSeatGradeId = gradeId++,
                    concertId = concert.concertId,
                    seatGrade = grade,
                    price = price
                )
            }
        }

        concertSeatGrades.forEach { seatGrade ->
            repository.save(seatGrade)
        }
    }

    private fun setUpInitialConcertDates(repository: MockConcertDateRepository, concerts: List<Concert>): List<ConcertDate> {
        val baseDate = LocalDateTime.of(2025, 7, 18, 19, 0) // 2025년 7월 18일 금요일 오후 7시

        var dateId = 1L
        val concertDates = concerts.flatMap { concert ->
            listOf(
                ConcertDate(
                    concertDateId = dateId++,
                    concertSession = 1L,
                    concertId = concert.concertId,
                    date = baseDate.plusWeeks((concert.concertId - 1) * 2),
                    isSoldOut = false
                ),
                ConcertDate(
                    concertDateId = dateId++,
                    concertSession = 2L,
                    concertId = concert.concertId,
                    date = baseDate.plusWeeks((concert.concertId - 1) * 2).plusDays(1),
                    isSoldOut = false
                ),
                ConcertDate(
                    concertDateId = dateId++,
                    concertSession = 3L,
                    concertId = concert.concertId,
                    date = baseDate.plusWeeks((concert.concertId - 1) * 2).plusDays(2),
                    isSoldOut = concert.concertId == 1L && dateId == 3L
                )
            )
        }

        concertDates.forEach { concertDate -> repository.save(concertDate) }
        return concertDates
    }

    private fun setUpInitialConcertSeats(repository: MockConcertSeatRepository, concertDates: List<ConcertDate>): List<ConcertSeat> {
        val seatGrades = listOf("STANDING", "VIP", "COMMON")
        val seatsPerGrade = 50 / 3

        var seatId = 1L
        val allSeats = mutableListOf<ConcertSeat>()

        concertDates.forEach { concertDate ->
            val seatsForDate = mutableListOf<ConcertSeat>()

            repeat(50) { seatIndex ->
                val seatNumber = (seatIndex + 1).toString()
                val gradeIndex = seatIndex / seatsPerGrade
                val seatGrade = when {
                    gradeIndex < seatGrades.size -> seatGrades[gradeIndex]
                    else -> seatGrades.last()
                }

                val seat = ConcertSeat(
                    concertSeatId = seatId++,
                    concertDateId = concertDate.concertDateId,
                    seatNumber = seatNumber,
                    seatGrade = seatGrade,
                    seatStatus = SeatStatus.AVAILABLE
                )
                seatsForDate.add(seat)
            }

            val shuffledSeats = seatsForDate.shuffled()
            val availableCount = (shuffledSeats.size * 0.6).toInt()
            val reservedCount = (shuffledSeats.size * 0.2).toInt()

            shuffledSeats.forEachIndexed { index, seat ->
                val status = when {
                    index < availableCount -> SeatStatus.AVAILABLE
                    index < availableCount + reservedCount -> SeatStatus.RESERVED
                    else -> SeatStatus.SOLD
                }

                val updatedSeat = ConcertSeat(
                    concertSeatId = seat.concertSeatId,
                    concertDateId = seat.concertDateId,
                    seatNumber = seat.seatNumber,
                    seatGrade = seat.seatGrade,
                    seatStatus = status
                )
                allSeats.add(updatedSeat)
            }
        }

        allSeats.forEach { seat ->
            repository.save(seat)
        }

        return allSeats
    }
}