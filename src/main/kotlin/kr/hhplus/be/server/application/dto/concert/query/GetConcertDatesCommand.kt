package kr.hhplus.be.server.application.dto.concert.query

data class GetConcertDatesCommand(
    val tokenId : String,
    val concertId: Long
)
