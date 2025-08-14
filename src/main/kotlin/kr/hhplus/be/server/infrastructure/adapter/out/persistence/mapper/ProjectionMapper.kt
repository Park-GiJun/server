package kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper

import kr.hhplus.be.server.application.dto.concert.PopularConcertDto
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.projection.PopularConcertProjection

object ProjectionMapper {
    fun popularConcertToDto(projection: PopularConcertProjection): PopularConcertDto {
        return PopularConcertDto(
            concertId = projection.concertId,
            concertName = projection.concertName,
            reservedCount = projection.reservedCount
        )
    }
}