package kr.hhplus.be.server.infrastructure.adapter.`in`.web.mock

import kr.hhplus.be.server.application.dto.event.ReservationEventDto
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.mock.dto.MockApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/mock/data-platform")
class MockDataPlatformController {

    private val log = LoggerFactory.getLogger(MockDataPlatformController::class.java)

    @PostMapping("/events")
    fun receiveEvent(@RequestBody event: ReservationEventDto): ResponseEntity<MockApiResponse> {
        log.info("""
            ========================================
            [MOCK DATA PLATFORM] 이벤트 수신 
            ========================================
            Type: ${event.eventType}
            ReservationId: ${event.reservationId}
            UserId: ${event.userId}
            ConcertId: ${event.concertId}
            SeatNumber: ${event.seatNumber}
            Price: ${event.price}원
            OccurredAt: ${event.occurredAt}
            ========================================
        """.trimIndent())

        val message = when(event.eventType) {
            "COMPLETED" -> "예약 완료 이벤트 처리 성공"
            "CANCELLED" -> "예약 취소 이벤트 처리 성공"
            else -> "이벤트 처리 성공"
        }
        return ResponseEntity.ok(
            MockApiResponse(
                success = true,
                message = message
            )
        )
    }
}