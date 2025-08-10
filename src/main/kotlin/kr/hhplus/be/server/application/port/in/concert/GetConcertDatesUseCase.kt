package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.GetConcertDatesQuery

interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult>
}