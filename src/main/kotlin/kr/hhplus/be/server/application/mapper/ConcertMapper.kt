package kr.hhplus.be.server.application.mapper

import kr.hhplus.be.server.application.dto.concert.result.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertResult
import kr.hhplus.be.server.application.dto.concert.result.ConcertSeatWithPriceResult
import kr.hhplus.be.server.domain.concert.*

object ConcertMapper {

    fun toResult(domain: Concert): ConcertResult {
        return ConcertResult(
            concertId = domain.concertId,
            concertName = domain.concertName,
            location = domain.location,
            description = domain.description
        )
    }

    fun toResults(domains: List<Concert>): List<ConcertResult> {
        return domains.map { toResult(it) }
    }

    fun toDateWithStatsResult(
        domain: ConcertDate,
        totalSeats: Int,
        availableSeats: Int
    ): ConcertDateWithStatsResult {
        return ConcertDateWithStatsResult(
            concertDateId = domain.concertDateId,
            concertId = domain.concertId,
            concertSession = domain.concertSession,
            date = domain.date,
            totalSeats = totalSeats,
            availableSeats = availableSeats,
            isSoldOut = domain.isSoldOut
        )
    }

    fun toSeatWithPriceResult(
        domain: ConcertSeat,
        price: Int
    ): ConcertSeatWithPriceResult {
        return ConcertSeatWithPriceResult(
            concertSeatId = domain.concertSeatId,
            concertDateId = domain.concertDateId,
            seatNumber = domain.seatNumber,
            seatGrade = domain.seatGrade,
            seatStatus = domain.seatStatus,
            price = price
        )
    }

    fun toSeatWithPriceResults(
        domains: List<ConcertSeat>,
        priceMap: Map<String, Int>
    ): List<ConcertSeatWithPriceResult> {
        return domains.map { seat ->
            toSeatWithPriceResult(seat, priceMap[seat.seatGrade] ?: 0)
        }
    }
}