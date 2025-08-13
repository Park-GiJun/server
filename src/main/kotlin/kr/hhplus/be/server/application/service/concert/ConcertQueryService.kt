package kr.hhplus.be.server.application.service.concert

import kr.hhplus.be.server.application.dto.concert.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.ConcertResult
import kr.hhplus.be.server.application.dto.concert.ConcertSeatWithPriceResult
import kr.hhplus.be.server.application.dto.concert.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.GetConcertSeatsQuery
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ConcertQueryService(
    private val concertRepository: ConcertRepository,
    private val concertDateRepository: ConcertDateRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val concertDomainService: ConcertDomainService
) : GetConcertListUseCase, GetConcertDatesUseCase, GetConcertSeatsUseCase {

    private val log = LoggerFactory.getLogger(ConcertQueryService::class.java)


    override fun getConcertList(): List<ConcertResult> {
        log.info("콘서트 목록 조회")
        val concerts = concertRepository.findConcertList()
        log.info("콘서트 목록 조회 완료: ${concerts.size}개")
        return ConcertMapper.toResults(concerts)
    }

    override fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult> {
        log.info("콘서트 날짜 조회: concertId=${command.concertId}")
        val concertDates = concertDateRepository.findByConcertId(command.concertId)

        if (concertDates.isEmpty()) {
            log.warn("콘서트 날짜 없음: concertId=${command.concertId}")
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

        log.info("콘서트 날짜 조회 완료: ${results.size}개 일정")
        return results
    }

    override fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult> {
        log.info("콘서트 좌석 조회: concertDateId=${command.concertDateId}")

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
        log.info("콘서트 좌석 조회 완료: ${results.size}석")

        return results
    }
}