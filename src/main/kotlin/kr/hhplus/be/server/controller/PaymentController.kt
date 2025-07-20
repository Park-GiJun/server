package kr.hhplus.be.server.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.dto.PaymentRequest
import kr.hhplus.be.server.dto.PaymentResponse
import kr.hhplus.be.server.dto.common.ApiResponse
import kr.hhplus.be.server.service.PaymentService
import kr.hhplus.be.server.util.JwtQueueTokenUtil
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
@Tag(name = "결제")
class PaymentController(
    private val paymentService: PaymentService,
    private val jwtQueueTokenUtil: JwtQueueTokenUtil
) {

    @PostMapping
    @Operation(summary = "결제 처리")
    fun processPayment(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: PaymentRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        val payment = paymentService.processPayment(tokenRequest, request)

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
    @Operation(summary = "결제 내역 조회")
    fun getPayment(
        @PathVariable paymentId: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        val payment = paymentService.getPayment(paymentId)

        if (payment.userId != tokenRequest.userId) {
            throw IllegalArgumentException("Access denied to this payment record")
        }

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
    @Operation(summary = "사용자 결제 내역 목록 조회")
    fun getUserPayments(
        @PathVariable userId: String,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<List<PaymentResponse>>> {
        val jwtToken = token.removePrefix("Bearer ")
        val tokenRequest = jwtQueueTokenUtil.parseToken(jwtToken)

        if (tokenRequest.userId != userId) {
            throw IllegalArgumentException("Access denied to this user's payment records")
        }

        val payments = paymentService.getUserPayments(userId)
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