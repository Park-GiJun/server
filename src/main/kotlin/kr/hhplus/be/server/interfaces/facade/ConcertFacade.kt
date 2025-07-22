package kr.hhplus.be.server.interfaces.facade

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesCommand
import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsCommand
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult
import org.springframework.stereotype.Component

@Component
class ConcertFacade(
    private val getConcertListUseCase: GetConcertListUseCase,
    private val getConcertDatesUseCase: GetConcertDatesUseCase,
    private val getConcertSeatsUseCase: GetConcertSeatsUseCase
) {

    fun getConcertList(): List<ConcertResult> {
        return getConcertListUseCase.getConcertList()
    }

    fun getConcertDates(command: GetConcertDatesCommand): List<ConcertDateWithStatsResult> {
        return getConcertDatesUseCase.getConcertDates(command)
    }

    fun getConcertDates(tokenId: String, concertId: Long): List<ConcertDateWithStatsResult> {
        val command = GetConcertDatesCommand(tokenId, concertId)
        return getConcertDatesUseCase.getConcertDates(command)
    }

    fun getConcertSeats(command: GetConcertSeatsCommand): List<ConcertSeatWithPriceResult> {
        return getConcertSeatsUseCase.getConcertSeats(command)
    }

    fun getConcertSeats(tokenId: String, concertDateId: Long): List<ConcertSeatWithPriceResult> {
        val command = GetConcertSeatsCommand(tokenId, concertDateId)
        return getConcertSeatsUseCase.getConcertSeats(command)
    }
}