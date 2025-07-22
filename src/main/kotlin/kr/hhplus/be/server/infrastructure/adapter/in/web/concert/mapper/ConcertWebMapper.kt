package kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.mapper

import kr.hhplus.be.server.application.dto.concert.query.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.query.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertDateResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertSeatResponse

object ConcertWebMapper {

    fun toGetConcertDatesCommand(tokenId: String, concertId: Long): GetConcertDatesQuery {
        return GetConcertDatesQuery(
            tokenId = tokenId,
            concertId = concertId
        )
    }

    fun toGetConcertSeatsCommand(tokenId: String, concertDateId: Long): GetConcertSeatsQuery {
        return GetConcertSeatsQuery(
            tokenId = tokenId,
            concertDateId = concertDateId
        )
    }

    fun toResponse(result: ConcertResult): ConcertResponse {
        return ConcertResponse(
            concertId = result.concertId,
            concertName = result.concertName,
            location = result.location,
            description = result.description
        )
    }

    fun toResponses(results: List<ConcertResult>): List<ConcertResponse> {
        return results.map { toResponse(it) }
    }

    fun toDateResponse(result: ConcertDateWithStatsResult): ConcertDateResponse {
        return ConcertDateResponse(
            concertDateId = result.concertDateId,
            concertId = result.concertId,
            concertSession = result.concertSession,
            date = result.date,
            totalSeats = result.totalSeats,
            availableSeats = result.availableSeats,
            isSoldOut = result.isSoldOut
        )
    }

    fun toDateResponses(results: List<ConcertDateWithStatsResult>): List<ConcertDateResponse> {
        return results.map { toDateResponse(it) }
    }

    fun toSeatResponse(result: ConcertSeatWithPriceResult): ConcertSeatResponse {
        return ConcertSeatResponse(
            concertSeatId = result.concertSeatId,
            concertDateId = result.concertDateId,
            seatNumber = result.seatNumber,
            seatGrade = result.seatGrade,
            seatStatus = result.seatStatus,
            price = result.price
        )
    }

    fun toSeatResponses(results: List<ConcertSeatWithPriceResult>): List<ConcertSeatResponse> {
        return results.map { toSeatResponse(it) }
    }
}