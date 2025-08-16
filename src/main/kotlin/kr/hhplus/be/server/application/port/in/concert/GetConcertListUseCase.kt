package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.ConcertResult

interface GetConcertListUseCase {
    fun getConcertList(): List<ConcertResult>
}