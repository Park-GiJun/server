package kr.hhplus.be.server.application.dto.concert.result

data class PopularConcertResult (
    val concertId: Long,
    val concertName: String,
    val reservedCount: Long
)