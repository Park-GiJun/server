package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesQuery

interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult>
}