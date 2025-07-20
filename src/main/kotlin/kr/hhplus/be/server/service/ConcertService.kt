package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.dto.ConcertDateWithStatsResponse
import kr.hhplus.be.server.dto.ConcertSeatWithPriceResponse
import kr.hhplus.be.server.exception.ConcertDateExpiredException
import kr.hhplus.be.server.exception.ConcertNotFoundException
import kr.hhplus.be.server.exception.ConcertSoldOutException
import kr.hhplus.be.server.repository.mock.MockConcertDateRepository
import kr.hhplus.be.server.repository.mock.MockConcertRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatGradeRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConcertService(
    private val concertRepository: MockConcertRepository,
    private val concertDateRepository: MockConcertDateRepository,
    private val concertSeatRepository: MockConcertSeatRepository,
    private val concertSeatGradeRepository: MockConcertSeatGradeRepository,
    private val queueService: QueueService
) {

    fun getConcertList(): List<Concert> {
        return concertRepository.findConcertList()
            ?: throw ConcertNotFoundException("No concerts found")
    }

    fun getConcertDate(tokenId: String, concertId: Long): List<ConcertDateWithStatsResponse> {
        // 토큰 검증 및 콘서트 ID 확인
        queueService.validateActiveTokenForConcert(tokenId, concertId)

        val concertDates = concertDateRepository.findConcertDateByConcertId(concertId)
            .ifEmpty { throw ConcertNotFoundException("No concert dates found for concert ID: $concertId") }

        return concertDates.map { concertDate ->
            val seats = concertSeatRepository.findConcertSeats(concertDate.concertDateId)
                ?: emptyList()

            val totalSeats = seats.size
            val availableSeats = seats.count { it.seatStatus == SeatStatus.AVAILABLE }

            ConcertDateWithStatsResponse(
                concertDate = concertDate,
                totalSeats = totalSeats,
                availableSeats = availableSeats
            )
        }
    }

    fun getConcertSeats(tokenId: String, concertDateId: Long): List<ConcertSeatWithPriceResponse> {
        // 토큰 검증
        val token = queueService.validateActiveToken(tokenId)

        val concertDate = concertDateRepository.findConcertDateByConcertDateId(concertDateId)
            ?: throw ConcertNotFoundException("Concert date not found: $concertDateId")

        val concert = concertRepository.findByConcertId(concertDate.concertId)
            ?: throw ConcertNotFoundException("Concert not found: ${concertDate.concertId}")

        // 토큰의 콘서트 ID와 요청된 콘서트 ID 일치 확인
        if (token.concertId != concertDate.concertId) {
            queueService.validateActiveTokenForConcert(tokenId, concertDate.concertId)
        }

        // 매진 확인
        if (concertDate.isSoldOut) {
            throw ConcertSoldOutException("Concert date is sold out")
        }

        // 날짜 만료 확인
        val now = LocalDateTime.now()
        if (concertDate.date.isBefore(now)) {
            throw ConcertDateExpiredException("Concert date has passed")
        }

        val seats = concertSeatRepository.findConcertSeats(concertDateId)
            ?: throw ConcertNotFoundException("No seats found for concert date: $concertDateId")

        val seatGradePriceMap = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
            .associateBy { it.seatGrade }

        return seats.map { seat ->
            val price = seatGradePriceMap[seat.seatGrade]?.price ?: 0

            ConcertSeatWithPriceResponse(
                seat = seat,
                price = price
            )
        }
    }
}