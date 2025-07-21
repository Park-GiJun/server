package kr.hhplus.be.server.application.service.concert

import kr.hhplus.be.server.application.dto.concert.command.GetConcertDatesCommand
import kr.hhplus.be.server.application.dto.concert.command.GetConcertSeatsCommand
import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesCommand
import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsCommand
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSoldOutException
import kr.hhplus.be.server.domain.concert.exception.ConcertDateExpiredException
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.application.dto.queue.command.ValidateTokenCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ConcertQueryService(
    private val concertRepository: ConcertRepository,
    private val concertDateRepository: ConcertDateRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val validateTokenUseCase: ValidateTokenUseCase
) : GetConcertListUseCase, GetConcertDatesUseCase, GetConcertSeatsUseCase {

    override fun getConcertList(): List<ConcertResult> {
        val concerts = concertRepository.findConcertList()

        return concerts.map { concert ->
            ConcertResult(
                concertId = concert.concertId,
                concertName = concert.concertName,
                location = concert.location,
                description = concert.description
            )
        }
    }

    override fun getConcertDates(command: GetConcertDatesCommand): List<ConcertDateWithStatsResult> {
        validateTokenUseCase.validateActiveTokenForConcert(
            ValidateTokenCommand(command.tokenId, command.concertId)
        )

        val concertDates = concertDateRepository.findByConcertId(command.concertId)

        if (concertDates.isEmpty()) {
            throw ConcertNotFoundException(command.concertId)
        }

        return concertDates.map { concertDate ->
            val seats = concertSeatRepository.findByConcertDateId(concertDate.concertDateId)
            val totalSeats = seats.size
            val availableSeats = seats.count { it.seatStatus == SeatStatus.AVAILABLE }

            ConcertDateWithStatsResult(
                concertDateId = concertDate.concertDateId,
                concertId = concertDate.concertId,
                concertSession = concertDate.concertSession,
                date = concertDate.date,
                totalSeats = totalSeats,
                availableSeats = availableSeats,
                isSoldOut = concertDate.isSoldOut
            )
        }
    }

    override fun getConcertSeats(command: GetConcertSeatsCommand): List<ConcertSeatWithPriceResult> {
        val token = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )

        val concertDate = concertDateRepository.findByConcertDateId(command.concertDateId)
            ?: throw ConcertNotFoundException(command.concertDateId)

        val concert = concertRepository.findByConcertId(concertDate.concertId)
            ?: throw ConcertNotFoundException(concertDate.concertId)

        if (token.concertId != concertDate.concertId) {
            validateTokenUseCase.validateActiveTokenForConcert(
                ValidateTokenCommand(command.tokenId, concertDate.concertId)
            )
        }

        if (concertDate.isSoldOut) {
            throw ConcertSoldOutException("Concert date is sold out")
        }

        val now = LocalDateTime.now()
        if (concertDate.date.isBefore(now)) {
            throw ConcertDateExpiredException("Concert date has passed")
        }

        val seats = concertSeatRepository.findByConcertDateId(command.concertDateId)
        if (seats.isEmpty()) {
            throw ConcertNotFoundException(command.concertDateId)
        }

        val seatGrades = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
        val seatGradePriceMap = seatGrades.associateBy { it.seatGrade }

        return seats.map { seat ->
            val price = seatGradePriceMap[seat.seatGrade]?.price ?: 0

            ConcertSeatWithPriceResult(
                concertSeatId = seat.concertSeatId,
                concertDateId = seat.concertDateId,
                seatNumber = seat.seatNumber,
                seatGrade = seat.seatGrade,
                seatStatus = seat.seatStatus,
                price = price
            )
        }
    }
}