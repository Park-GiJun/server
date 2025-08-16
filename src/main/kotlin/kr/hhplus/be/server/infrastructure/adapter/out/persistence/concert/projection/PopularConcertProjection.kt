package kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.projection

interface PopularConcertProjection {
    val concertId: Long
    val concertName: String
    val reservedCount: Long
}