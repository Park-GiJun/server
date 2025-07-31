package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.dto.concert.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.GetConcertSeatsQuery
import kr.hhplus.be.server.application.port.`in`.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.service.concert.ConcertQueryService
import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class ConcertQueryServiceTest {

    private val concertRepository: ConcertRepository = mockk()
    private val concertDateRepository: ConcertDateRepository = mockk()
    private val concertSeatRepository: ConcertSeatRepository = mockk()
    private val concertSeatGradeRepository: ConcertSeatGradeRepository = mockk()

    private val getConcertListUseCase: GetConcertListUseCase = ConcertQueryService(
        concertRepository, concertDateRepository, concertSeatRepository, concertSeatGradeRepository
    )
    private val getConcertDatesUseCase: GetConcertDatesUseCase = ConcertQueryService(
        concertRepository, concertDateRepository, concertSeatRepository, concertSeatGradeRepository
    )
    private val getConcertSeatsUseCase: GetConcertSeatsUseCase = ConcertQueryService(
        concertRepository, concertDateRepository, concertSeatRepository, concertSeatGradeRepository
    )

    @Nested
    @DisplayName("콘서트 목록 조회")
    inner class GetConcertList {

        @Test
        @DisplayName("콘서트 목록 조회 성공")
        fun getConcertListSuccess() {
            val concerts = listOf(
                Concert(1L, "아이유 콘서트", "올림픽공원", "2024 아이유 콘서트"),
                Concert(2L, "BTS 콘서트", "잠실경기장", "2024 BTS 콘서트")
            )

            every { concertRepository.findConcertList() } returns concerts

            val result = getConcertListUseCase.getConcertList()

            assertThat(result).hasSize(2)
            assertThat(result[0].concertName).isEqualTo("아이유 콘서트")
            assertThat(result[1].concertName).isEqualTo("BTS 콘서트")

            verify { concertRepository.findConcertList() }
        }

        @Test
        @DisplayName("콘서트가 없는 경우 빈 목록 반환")
        fun getConcertListEmpty() {
            every { concertRepository.findConcertList() } returns emptyList()

            val result = getConcertListUseCase.getConcertList()

            assertThat(result).isEmpty()
            verify { concertRepository.findConcertList() }
        }
    }

    @Nested
    @DisplayName("콘서트 날짜 조회")
    inner class GetConcertDates {

        @Test
        @DisplayName("콘서트 날짜 조회 성공")
        fun getConcertDatesSuccess() {
            val concertId = 1L
            val query = GetConcertDatesQuery("token123", concertId)
            val concertDates = listOf(
                ConcertDate(1L, 1L, concertId, LocalDateTime.now().plusDays(7), false),
                ConcertDate(2L, 2L, concertId, LocalDateTime.now().plusDays(8), false)
            )
            val seats = listOf(
                ConcertSeat(1L, 1L, "A1", "VIP", SeatStatus.AVAILABLE),
                ConcertSeat(2L, 1L, "A2", "VIP", SeatStatus.RESERVED)
            )

            every { concertDateRepository.findByConcertId(concertId) } returns concertDates
            every { concertSeatRepository.findByConcertDateId(1L) } returns seats
            every { concertSeatRepository.findByConcertDateId(2L) } returns seats

            val result = getConcertDatesUseCase.getConcertDates(query)

            assertThat(result).hasSize(2)
            assertThat(result[0].totalSeats).isEqualTo(2)
            assertThat(result[0].availableSeats).isEqualTo(1)

            verify { concertDateRepository.findByConcertId(concertId) }
            verify { concertSeatRepository.findByConcertDateId(1L) }
            verify { concertSeatRepository.findByConcertDateId(2L) }
        }

        @Test
        @DisplayName("존재하지 않는 콘서트 날짜 조회 실패")
        fun getConcertDatesNotFound() {
            val concertId = 999L
            val query = GetConcertDatesQuery("token123", concertId)

            every { concertDateRepository.findByConcertId(concertId) } returns emptyList()

            assertThrows<ConcertNotFoundException> {
                getConcertDatesUseCase.getConcertDates(query)
            }

            verify { concertDateRepository.findByConcertId(concertId) }
        }
    }

    @Nested
    @DisplayName("콘서트 좌석 조회")
    inner class GetConcertSeats {

        @Test
        @DisplayName("콘서트 좌석 조회 성공")
        fun getConcertSeatsSuccess() {
            val concertDateId = 1L
            val concertId = 1L
            val query = GetConcertSeatsQuery("token123", concertDateId)

            val concertDate = ConcertDate(
                concertDateId, 1L, concertId,
                LocalDateTime.now().plusDays(7), false
            )
            val concert = Concert(concertId, "아이유 콘서트", "올림픽공원", "2024 아이유 콘서트")
            val seats = listOf(
                ConcertSeat(1L, concertDateId, "A1", "VIP", SeatStatus.AVAILABLE),
                ConcertSeat(2L, concertDateId, "A2", "COMMON", SeatStatus.AVAILABLE)
            )
            val seatGrades = listOf(
                ConcertSeatGrade(1L, concertId, "VIP", 170000),
                ConcertSeatGrade(2L, concertId, "COMMON", 120000)
            )

            every { concertDateRepository.findByConcertDateId(concertDateId) } returns concertDate
            every { concertRepository.findByConcertId(concertId) } returns concert
            every { concertSeatRepository.findByConcertDateId(concertDateId) } returns seats
            every { concertSeatGradeRepository.findByConcertId(concertId) } returns seatGrades

            val result = getConcertSeatsUseCase.getConcertSeats(query)

            assertThat(result).hasSize(2)
            assertThat(result[0].seatGrade).isEqualTo("VIP")
            assertThat(result[0].price).isEqualTo(170000)
            assertThat(result[1].seatGrade).isEqualTo("COMMON")
            assertThat(result[1].price).isEqualTo(120000)

            verify { concertDateRepository.findByConcertDateId(concertDateId) }
            verify { concertRepository.findByConcertId(concertId) }
            verify { concertSeatRepository.findByConcertDateId(concertDateId) }
            verify { concertSeatGradeRepository.findByConcertId(concertId) }
        }

        @Test
        @DisplayName("존재하지 않는 콘서트 날짜의 좌석 조회 실패")
        fun getConcertSeatsDateNotFound() {
            val concertDateId = 999L
            val query = GetConcertSeatsQuery("token123", concertDateId)

            every { concertDateRepository.findByConcertDateId(concertDateId) } returns null

            assertThrows<ConcertNotFoundException> {
                getConcertSeatsUseCase.getConcertSeats(query)
            }

            verify { concertDateRepository.findByConcertDateId(concertDateId) }
        }

        @Test
        @DisplayName("좌석이 없는 콘서트 날짜 조회 실패")
        fun getConcertSeatsEmptySeats() {
            val concertDateId = 1L
            val concertId = 1L
            val query = GetConcertSeatsQuery("token123", concertDateId)

            val concertDate = ConcertDate(
                concertDateId, 1L, concertId,
                LocalDateTime.now().plusDays(7), false
            )
            val concert = Concert(concertId, "아이유 콘서트", "올림픽공원", "2024 아이유 콘서트")

            every { concertDateRepository.findByConcertDateId(concertDateId) } returns concertDate
            every { concertRepository.findByConcertId(concertId) } returns concert
            every { concertSeatRepository.findByConcertDateId(concertDateId) } returns emptyList()

            assertThrows<ConcertNotFoundException> {
                getConcertSeatsUseCase.getConcertSeats(query)
            }

            verify { concertDateRepository.findByConcertDateId(concertDateId) }
            verify { concertRepository.findByConcertId(concertId) }
            verify { concertSeatRepository.findByConcertDateId(concertDateId) }
        }
    }
}