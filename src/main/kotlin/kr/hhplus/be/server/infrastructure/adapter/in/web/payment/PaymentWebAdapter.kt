package kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment

import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto.PaymentRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto.PaymentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.saga.payment.PaymentSagaOrchestrator
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제", description = "결제 처리 API")
class PaymentWebAdapter(
    private val paymentSagaOrchestrator: PaymentSagaOrchestrator
) {

    private val log = LoggerFactory.getLogger(PaymentWebAdapter::class.java)

    @PostMapping
    @Operation(
        summary = "결제 처리",
        description = "Saga 패턴으로 결제를 처리합니다 (포인트 차감 → 좌석 확정 → 예약 확정 → 결제 생성)"
    )
    fun processPayment(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: PaymentRequest
    ): ApiResponse<PaymentResponse> {
        val sagaId = paymentSagaOrchestrator.startPaymentSaga(
            reservationId = request.reservationId,
            userId = request.userId,
            seatId = request.seatId,
            concertDateId = request.concertDateId,
            pointsToUse = request.pointsToUse,
            totalAmount = request.totalAmount
        )

        return ApiResponse.success(
            PaymentResponse(
                sagaId = sagaId,
                status = "PROCESSING",
                message = "결제가 처리 중입니다. 상태를 확인해주세요."
            )
        )
    }
}
