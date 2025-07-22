package kr.hhplus.be.server.application.dto.concert

data class GetConcertDatesQuery(
    val tokenId : String,
    val concertId: Long
)
data class GetConcertQuery (
    val concertId: Long
)
data class GetConcertSeatsQuery(
    val tokenId: String,
    val concertDateId: Long
)