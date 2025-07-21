package kr.hhplus.be.server.application.dto.concert.result

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