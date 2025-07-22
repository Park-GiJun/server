package kr.hhplus.be.server.application.dto.concert.query

data class GetConcertDatesQuery(
    val tokenId : String,
    val concertId: Long
)
