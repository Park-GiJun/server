package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult


interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult>
}