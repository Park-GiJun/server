package kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertDateResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto.ConcertSeatResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.mapper.ConcertWebMapper
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/concerts")
@Tag(name = "콘서트", description = "콘서트 정보 조회 API")
class ConcertWebAdapter(
    private val getConcertListUseCase: GetConcertListUseCase,
    private val getConcertDatesUseCase: GetConcertDatesUseCase,
    private val getConcertSeatsUseCase: GetConcertSeatsUseCase,
    private val validateTokenUseCase: ValidateTokenUseCase
) {

    @GetMapping
    @Operation(
        summary = "전체 콘서트 목록 조회",
        description = "등록된 모든 콘서트의 기본 정보를 조회합니다. 대기열 토큰이 필요하지 않습니다."
    )
    fun getConcertList(): ResponseEntity<ApiResponse<List<ConcertResponse>>> {
        val concerts = getConcertListUseCase.getConcertList()
        val response = ConcertWebMapper.toResponses(concerts)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert list retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/{concertId}/dates")
    @Operation(
        summary = "특정 콘서트의 예약 가능 날짜 조회",
        description = "지정된 콘서트의 예약 가능한 날짜 목록을 조회합니다. 활성화된 대기열 토큰이 필요합니다."
    )
    fun getConcertDates(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<List<ConcertDateResponse>>> {

        validateTokenUseCase.validateActiveTokenForConcert(
            ValidateTokenCommand(tokenId, concertId)
        )

        val command = ConcertWebMapper.toGetConcertDatesCommand(tokenId, concertId)
        val concertDatesWithStats = getConcertDatesUseCase.getConcertDates(command)
        val response = ConcertWebMapper.toDateResponses(concertDatesWithStats)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert dates retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/{concertId}/dates/{dateId}/seats")
    @Operation(
        summary = "특정 날짜의 좌석 조회",
        description = "지정된 콘서트 날짜의 좌석 정보와 가격을 조회합니다. 활성화된 대기열 토큰이 필요합니다."
    )
    fun getConcertSeats(
        @Parameter(description = "콘서트 ID", example = "1")
        @PathVariable concertId: Long,
        @Parameter(description = "콘서트 날짜 ID", example = "1")
        @PathVariable dateId: Long,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<List<ConcertSeatResponse>>> {

        validateTokenUseCase.validateActiveTokenForConcert(
            ValidateTokenCommand(tokenId, concertId)
        )

        val command = ConcertWebMapper.toGetConcertSeatsCommand(tokenId, dateId)
        val concertSeatsWithPrice = getConcertSeatsUseCase.getConcertSeats(command)
        val response = ConcertWebMapper.toSeatResponses(concertSeatsWithPrice)

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Concert seats retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}