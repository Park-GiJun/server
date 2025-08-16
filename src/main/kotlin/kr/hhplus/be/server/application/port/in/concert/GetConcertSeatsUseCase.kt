package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.*

interface GetConcertSeatsUseCase {
    fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult>
}