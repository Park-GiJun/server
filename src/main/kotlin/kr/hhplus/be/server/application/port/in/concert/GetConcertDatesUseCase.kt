package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesCommand
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult


interface GetConcertDatesUseCase {
    fun getConcertDates(command: GetConcertDatesCommand): List<ConcertDateWithStatsResult>
}