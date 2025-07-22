package kr.hhplus.be.server.application.dto.concert

import kr.hhplus.be.server.domain.concert.SeatStatus
import java.time.LocalDateTime

data class ConcertDateResult (
    val concertDateId: Long,
    val concertId: Long,
    val concertSession: Long,
    val date: LocalDateTime,
    val totalSeats: Int,
    val availableSeats: Int,
    val isSoldOut: Boolean
)

data class ConcertDateWithStatsResult(
    val concertDateId: Long,
    val concertId: Long,
    val concertSession: Long,
    val date: LocalDateTime,
    val totalSeats: Int,
    val availableSeats: Int,
    val isSoldOut: Boolean
)

data class ConcertResult(
    val concertId: Long,
    val concertName: String,
    val location: String,
    val description: String?
)

data class ConcertSeatWithPriceResult(
    val concertSeatId: Long,
    val concertDateId: Long,
    val seatNumber: String,
    val seatGrade: String,
    val seatStatus: SeatStatus,
    val price: Int
)