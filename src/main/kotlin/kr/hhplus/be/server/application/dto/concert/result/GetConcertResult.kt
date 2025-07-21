package kr.hhplus.be.server.application.dto.concert.result

data class GetConcertResult (
    val concertId: String,
    val concertName : String,
    val location: String,
    val description : String?
)