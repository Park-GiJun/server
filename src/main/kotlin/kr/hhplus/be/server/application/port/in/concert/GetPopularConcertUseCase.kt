package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.PopularConcertDto

interface GetPopularConcertUseCase {
    fun getPopularConcert(limit: Int): List<PopularConcertDto>
}