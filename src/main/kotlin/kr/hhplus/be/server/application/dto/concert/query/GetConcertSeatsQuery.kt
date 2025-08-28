package kr.hhplus.be.server.application.dto.concert.query

data class GetConcertSeatsQuery(
    val tokenId: String,
    val concertDateId: Long
)