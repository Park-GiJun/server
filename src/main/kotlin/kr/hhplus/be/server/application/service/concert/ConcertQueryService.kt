package kr.hhplus.be.server.application.service.concert

import kr.hhplus.be.server.application.dto.concert.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.ConcertResult
import kr.hhplus.be.server.application.dto.concert.ConcertSeatWithPriceResult
import kr.hhplus.be.server.application.dto.concert.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.PopularConcertDto
import kr.hhplus.be.server.application.mapper.ConcertMapper
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.domain.concert.ConcertDomainService
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetPopularConcertUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ConcertQueryService(
    private val concertRepository: ConcertRepository,
    private val concertDateRepository: ConcertDateRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository
) : GetConcertListUseCase, GetConcertDatesUseCase, GetConcertSeatsUseCase, GetPopularConcertUseCase {
    private val concertDomainService= ConcertDomainService()

    override fun getConcertList(): List<ConcertResult> {
        val concerts = concertRepository.findConcertList()
        return ConcertMapper.toResults(concerts)
    }

    override fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult> {
        val concertDates = concertDateRepository.findByConcertId(command.concertId)
        if (concertDates.isEmpty()) {
            throw ConcertNotFoundException(command.concertId)
        }
        val results = concertDates.map { concertDate ->
            val seats = concertSeatRepository.findByConcertDateId(concertDate.concertDateId)
            val (totalSeats, availableSeats) = concertDomainService.calculateSeatStatistics(seats)
            ConcertMapper.toDateWithStatsResult(
                domain = concertDate,
                totalSeats = totalSeats,
                availableSeats = availableSeats
            )
        }
        return results
    }

    override fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult> {
        val concertDate = concertDateRepository.findByConcertDateId(command.concertDateId)
        concertDomainService.validateConcertDateExists(concertDate, command.concertDateId)
        val concert = concertRepository.findByConcertId(concertDate!!.concertId)
        concertDomainService.validateConcertExists(concert)
        concertDomainService.validateConcertAvailability(concertDate, concert!!.concertName)
        val seats = concertSeatRepository.findByConcertDateId(command.concertDateId)
        concertDomainService.validateSeatsExist(seats, command.concertDateId)
        val seatGrades = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
        val seatGradePriceMap = concertDomainService.buildSeatPriceMap(seatGrades)
        val results = ConcertMapper.toSeatWithPriceResults(seats, seatGradePriceMap)
        return results
    }

    override fun getPopularConcert(limit: Int): List<PopularConcertDto> {
        return concertRepository.findByPopularConcert(5)
    }
}