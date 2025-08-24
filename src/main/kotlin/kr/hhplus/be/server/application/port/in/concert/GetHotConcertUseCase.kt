package kr.hhplus.be.server.application.port.`in`.concert

import kr.hhplus.be.server.application.dto.concert.PopularConcertDto

interface GetHotConcertUseCase {
    fun getHotConcert(limit: Int): List<PopularConcertDto>
    fun increaseConcert(concertId: Long)
}