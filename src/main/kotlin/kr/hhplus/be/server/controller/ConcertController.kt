package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.ConcertDateResponse
import kr.hhplus.be.server.dto.ConcertResponse
import kr.hhplus.be.server.dto.ConcertSeatResponse
import kr.hhplus.be.server.dto.common.ApiResponse
import kr.hhplus.be.server.service.ConcertService
import kr.hhplus.be.server.util.JwtQueueTokenUtil
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/concerts")
@Tag(name = "콘서트")
class ConcertController(
    private val concertService: ConcertService, private val jwtQueueTokenUtil: JwtQueueTokenUtil
) {

    @GetMapping
    @Operation(summary = "전체 콘서트 목록 조회")
    fun getConcertList(): ResponseEntity<ApiResponse<List<ConcertResponse>>> {
        val concerts = concertService.getConcertList()
        val response = concerts.map { concert ->
            ConcertResponse(
                concertId = concert.concertId,
                concertName = concert.concertName,
                location = concert.location,
                description = concert.description
            )
        }

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert list retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/{concertId}/dates")
    @Operation(summary = "특정 콘서트의 예약 가능 날짜 조회")
    fun getConcertDates(
        @PathVariable concertId: Long, @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<List<ConcertDateResponse>>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        if (tokenRequest.concertId != concertId) {
            throw IllegalArgumentException("Token concert ID does not match requested concert ID")
        }

        val concertDatesWithStats = concertService.getConcertDate(tokenRequest, concertId)
        val response = concertDatesWithStats.map { dateWithStats ->
            ConcertDateResponse(
                concertDateId = dateWithStats.concertDate.concertDateId,
                concertId = dateWithStats.concertDate.concertId,
                concertSession = dateWithStats.concertDate.concertSession,
                date = dateWithStats.concertDate.date,
                totalSeats = dateWithStats.totalSeats,
                availableSeats = dateWithStats.availableSeats,
                isSoldOut = dateWithStats.concertDate.isSoldOut
            )
        }

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert dates retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/{concertId}/dates/{dateId}/seats")
    @Operation(summary = "특정 날짜의 좌석 조회")
    fun getConcertSeats(
        @PathVariable concertId: Long, @PathVariable dateId: Long, @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<List<ConcertSeatResponse>>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        if (tokenRequest.concertId != concertId) {
            throw IllegalArgumentException("Token concert ID does not match requested concert ID")
        }

        val concertSeatsWithPrice = concertService.getConcertSeats(tokenRequest, dateId)
        val response = concertSeatsWithPrice.map { seatWithPrice ->
            ConcertSeatResponse(
                concertSeatId = seatWithPrice.seat.concertSeatId,
                concertDateId = seatWithPrice.seat.concertDateId,
                seatNumber = seatWithPrice.seat.seatNumber,
                seatGrade = seatWithPrice.seat.seatGrade,
                seatStatus = seatWithPrice.seat.seatStatus,
                price = seatWithPrice.price
            )
        }

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert seats retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}