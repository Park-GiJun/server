package kr.hhplus.be.server.application.dto.concert

data class PopularConcertDto (
    val concertId: Long,
    val concertName: String,
    val reservedCount: Long
)