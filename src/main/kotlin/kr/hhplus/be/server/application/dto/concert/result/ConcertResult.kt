package kr.hhplus.be.server.application.dto.concert.result

data class ConcertResult(
    val concertId: Long,
    val concertName: String,
    val location: String,
    val description: String?
)