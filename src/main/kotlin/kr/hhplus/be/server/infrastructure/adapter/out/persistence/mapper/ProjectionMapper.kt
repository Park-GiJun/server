package kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper

import kr.hhplus.be.server.application.dto.concert.result.PopularConcertResult
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.projection.PopularConcertProjection

object ProjectionMapper {
    fun popularConcertToDto(projection: PopularConcertProjection): PopularConcertResult {
        return PopularConcertResult(
            concertId = projection.concertId,
            concertName = projection.concertName,
            reservedCount = projection.reservedCount
        )
    }
}