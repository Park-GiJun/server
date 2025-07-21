package kr.hhplus.be.server.application.dto.concert.query

data class GetConcertSeatsCommand(
    val tokenId: String,
    val concertDateId: Long
)