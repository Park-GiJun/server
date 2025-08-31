package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult

interface GetConcertSeatsUseCase {
    fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult>
}