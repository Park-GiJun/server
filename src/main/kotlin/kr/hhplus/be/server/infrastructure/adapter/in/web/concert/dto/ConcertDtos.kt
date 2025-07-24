package kr.hhplus.be.server.infrastructure.adapter.`in`.web.concert.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.domain.concert.SeatStatus
import java.time.LocalDateTime

data class ConcertResponse(
    @field:Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,

    @field:Schema(description = "콘서트 이름", example = "아이유 콘서트")
    val concertName: String,

    @field:Schema(description = "공연 장소", example = "서울 올림픽공원")
    val location: String,

    @field:Schema(description = "콘서트 설명", example = "아이유 2025 콘서트")
    val description: String? = null
)

data class ConcertDateResponse(
    @field:Schema(description = "콘서트 날짜 ID", example = "1")
    val concertDateId: Long,

    @field:Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,

    @field:Schema(description = "콘서트 세션", example = "1")
    val concertSession: Long,

    @field:Schema(description = "공연 날짜", example = "2025-07-18T19:00:00")
    val date: LocalDateTime,

    @field:Schema(description = "전체 좌석 수", example = "50")
    val totalSeats: Int,

    @field:Schema(description = "예약 가능 좌석 수", example = "30")
    val availableSeats: Int,

    @field:Schema(description = "매진 여부", example = "false")
    val isSoldOut: Boolean
)

data class ConcertSeatResponse(
    @field:Schema(description = "좌석 ID", example = "1")
    val concertSeatId: Long,

    @field:Schema(description = "콘서트 날짜 ID", example = "1")
    val concertDateId: Long,

    @field:Schema(description = "좌석 번호", example = "A1")
    val seatNumber: String,

    @field:Schema(description = "좌석 등급", example = "VIP", allowableValues = ["STANDING", "VIP", "COMMON"])
    val seatGrade: String,

    @field:Schema(description = "좌석 상태", example = "AVAILABLE")
    val seatStatus: SeatStatus,

    @field:Schema(description = "좌석 가격", example = "170000")
    val price: Int
)