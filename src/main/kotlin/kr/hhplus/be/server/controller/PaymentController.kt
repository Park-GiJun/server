package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto.PaymentRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.payment.dto.PaymentResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.common.ApiResponse
import kr.hhplus.be.server.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
@Tag(name = "결제", description = "결제 처리 및 조회 API")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping
    @Operation(
        summary = "결제 처리",
        description = "임시 예약에 대한 결제를 처리합니다. 결제 완료 후 예약이 확정되고 대기열 토큰이 완료 상태가 됩니다."
    )
    fun processPayment(
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("Queue-Token") tokenId: String,
        @RequestBody request: PaymentRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {

        val payment = paymentService.processPayment(tokenId, request)

        val response = PaymentResponse(
            paymentId = payment.paymentId,
            reservationId = payment.reservationId,
            totalAmount = payment.totalAmount,
            pointsUsed = payment.discountAmount,
            actualAmount = payment.actualAmount,
            paymentAt = payment.paymentAt,
            message = "Payment completed successfully"
        )

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Payment processed successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/{paymentId}")
    @Operation(
        summary = "결제 내역 조회",
        description = "특정 결제 내역의 상세 정보를 조회합니다. 토큰 소유자만 조회 가능합니다."
    )
    fun getPayment(
        @Parameter(description = "결제 ID", example = "1")
        @PathVariable paymentId: Long,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<PaymentResponse>> {

        val payment = paymentService.getPayment(tokenId, paymentId)

        val response = PaymentResponse(
            paymentId = payment.paymentId,
            reservationId = payment.reservationId,
            totalAmount = payment.totalAmount,
            pointsUsed = payment.discountAmount,
            actualAmount = payment.actualAmount,
            paymentAt = payment.paymentAt,
            message = "Payment details retrieved"
        )

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "Payment details retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "사용자 결제 내역 목록 조회",
        description = "특정 사용자의 모든 결제 내역을 조회합니다. 토큰 소유자만 조회 가능합니다."
    )
    fun getUserPayments(
        @Parameter(description = "사용자 ID", example = "user-1")
        @PathVariable userId: String,
        @Parameter(description = "대기열 토큰 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("Queue-Token") tokenId: String
    ): ResponseEntity<ApiResponse<List<PaymentResponse>>> {

        val payments = paymentService.getUserPayments(tokenId, userId)
        val response = payments.map { payment ->
            PaymentResponse(
                paymentId = payment.paymentId,
                reservationId = payment.reservationId,
                totalAmount = payment.totalAmount,
                pointsUsed = payment.discountAmount,
                actualAmount = payment.actualAmount,
                paymentAt = payment.paymentAt,
                message = "Payment record"
            )
        }

        val apiResponse = ApiResponse(
            success = true,
            status = HttpStatus.OK.value(),
            data = response,
            message = "User payment history retrieved successfully"
        )

        return ResponseEntity.ok(apiResponse)
    }
}