package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.concert.*

interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult>
}

interface GetConcertListUseCase {
    fun getConcertList(): List<ConcertResult>
}

interface GetConcertSeatsUseCase {
    fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult>
}