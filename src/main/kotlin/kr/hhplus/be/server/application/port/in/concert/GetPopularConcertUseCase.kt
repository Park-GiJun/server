package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.result.PopularConcertResult

interface GetPopularConcertUseCase {
    fun getPopularConcert(limit: Int): List<PopularConcertResult>
}