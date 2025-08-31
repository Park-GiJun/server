package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.result.PopularConcertResult

interface GetHotConcertUseCase {
    fun getHotConcert(limit: Int): List<PopularConcertResult>
    fun increaseConcert(concertId: Long)
}