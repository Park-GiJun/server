package kr.hhplus.be.server.application.service.concert

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult
import kr.hhplus.be.server.application.mapper.ConcertMapper
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
        return ConcertMapper.toResults(concerts)
    }

    override fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult> {
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

            ConcertMapper.toDateWithStatsResult(
                domain = concertDate,
                totalSeats = totalSeats,
                availableSeats = availableSeats
            )
        }
    }

    override fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult> {
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
            throw ConcertSoldOutException(concert.concertName)
        }

        val now = LocalDateTime.now()
        if (concertDate.date.isBefore(now)) {
            throw ConcertDateExpiredException(concertDate.date)
        }

        val seats = concertSeatRepository.findByConcertDateId(command.concertDateId)
        if (seats.isEmpty()) {
            throw ConcertNotFoundException(command.concertDateId)
        }

        val seatGrades = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
        val seatGradePriceMap = seatGrades.associateBy({ it.seatGrade }, { it.price })

        return ConcertMapper.toSeatWithPriceResults(seats, seatGradePriceMap)
    }
}