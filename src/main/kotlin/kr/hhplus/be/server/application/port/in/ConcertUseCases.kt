package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult

interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult>
}

interface GetConcertListUseCase {
    fun getConcertList(): List<ConcertResult>
}

interface GetConcertSeatsUseCase {
    fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult>
}