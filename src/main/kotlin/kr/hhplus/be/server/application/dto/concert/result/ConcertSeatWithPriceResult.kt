package kr.hhplus.be.server.application.dto.concert.result

import kr.hhplus.be.server.domain.concert.SeatStatus

data class ConcertSeatWithPriceResult(
    val concertSeatId: Long,
    val concertDateId: Long,
    val seatNumber: String,
    val seatGrade: String,
    val seatStatus: SeatStatus,
    val price: Int
)