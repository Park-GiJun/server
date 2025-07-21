package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.result.ConcertResult

interface GetConcertListUseCase {
    fun getConcertList(): List<ConcertResult>
}