package kr.hhplus.be.server.infrastructure.adapter.`in`.web.exception

import kr.hhplus.be.server.domain.common.exception.*
import kr.hhplus.be.server.domain.user.exception.*
import kr.hhplus.be.server.domain.queue.exception.*
import kr.hhplus.be.server.domain.concert.exception.*
import kr.hhplus.be.server.domain.reservation.exception.*
import kr.hhplus.be.server.domain.payment.exception.*
import kr.hhplus.be.server.dto.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class WebExceptionHandler {

    private val log = LoggerFactory.getLogger(WebExceptionHandler::class.java)

    // ======== Domain Exceptions ========

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Entity not found: ${ex.message}")
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found")
    }

    @ExceptionHandler(BusinessRuleViolationException::class)
    fun handleBusinessRuleViolationException(ex: BusinessRuleViolationException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Business rule violation: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Business rule violation")
    }

    @ExceptionHandler(EntityStateException::class)
    fun handleEntityStateException(ex: EntityStateException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid entity state: ${ex.message}")
        return createErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Invalid entity state")
    }

    // ======== User Domain Exceptions ========

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(ex: UserNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.warn("User not found: ${ex.message}")
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "User not found")
    }

    @ExceptionHandler(InsufficientPointException::class)
    fun handleInsufficientPointException(ex: InsufficientPointException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Insufficient point: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Insufficient point")
    }

    @ExceptionHandler(InvalidPointAmountException::class)
    fun handleInvalidPointAmountException(ex: InvalidPointAmountException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid point amount: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid point amount")
    }

    // ======== Queue Domain Exceptions ========

    @ExceptionHandler(QueueTokenNotFoundException::class)
    fun handleQueueTokenNotFoundException(ex: QueueTokenNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Queue token not found: ${ex.message}")
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Queue token not found")
    }

    @ExceptionHandler(InvalidTokenStatusException::class)
    fun handleInvalidTokenStatusException(ex: InvalidTokenStatusException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid token status: ${ex.message}")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Invalid token status")
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid token: ${ex.message}")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Invalid token")
    }

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpiredException(ex: TokenExpiredException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Token expired: ${ex.message}")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Token expired")
    }

    // ======== Concert Domain Exceptions ========

    @ExceptionHandler(ConcertNotFoundException::class)
    fun handleConcertNotFoundException(ex: ConcertNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Concert not found: ${ex.message}")
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Concert not found")
    }

    @ExceptionHandler(SeatAlreadyBookedException::class)
    fun handleSeatAlreadyBookedException(ex: SeatAlreadyBookedException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Seat already booked: ${ex.message}")
        return createErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Seat already booked")
    }

    @ExceptionHandler(ConcertSoldOutException::class)
    fun handleConcertSoldOutException(ex: ConcertSoldOutException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Concert sold out: ${ex.message}")
        return createErrorResponse(HttpStatus.GONE, ex.message ?: "Concert is sold out")
    }

    @ExceptionHandler(ConcertDateExpiredException::class)
    fun handleConcertDateExpiredException(ex: ConcertDateExpiredException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Concert date expired: ${ex.message}")
        return createErrorResponse(HttpStatus.GONE, ex.message ?: "Concert date has expired")
    }

    // ======== Reservation Domain Exceptions ========

    @ExceptionHandler(TempReservationNotFoundException::class)
    fun handleTempReservationNotFoundException(ex: TempReservationNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Temp reservation not found: ${ex.message}")
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Temporary reservation not found")
    }

    @ExceptionHandler(ReservationExpiredException::class)
    fun handleReservationExpiredException(ex: ReservationExpiredException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Reservation expired: ${ex.message}")
        return createErrorResponse(HttpStatus.GONE, ex.message ?: "Reservation has expired")
    }

    @ExceptionHandler(InvalidReservationStatusException::class)
    fun handleInvalidReservationStatusException(ex: InvalidReservationStatusException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid reservation status: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid reservation status")
    }

    // ======== Payment Domain Exceptions ========

    @ExceptionHandler(PaymentAlreadyProcessedException::class)
    fun handlePaymentAlreadyProcessedException(ex: PaymentAlreadyProcessedException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Payment already processed: ${ex.message}")
        return createErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Payment already processed")
    }

    @ExceptionHandler(InvalidPaymentAmountException::class)
    fun handleInvalidPaymentAmountException(ex: InvalidPaymentAmountException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid payment amount: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid payment amount")
    }

    // ======== Generic Exceptions ========

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid argument: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid argument")
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Invalid state: ${ex.message}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid state")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Any>> {
        log.error("Unexpected error occurred: ${ex.message}", ex)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    // ======== Helper Methods ========

    private fun createErrorResponse(status: HttpStatus, message: String): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse<Any>(
            success = false,
            status = status.value(),
            data = null,
            message = message
        )
        return ResponseEntity(response, status)
    }
}