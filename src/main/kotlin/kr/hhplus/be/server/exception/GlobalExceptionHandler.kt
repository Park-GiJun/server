package kr.hhplus.be.server.exception

import kr.hhplus.be.server.dto.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class UserNotFoundException(message: String) : RuntimeException(message)
class QueueTokenNotFoundException(message: String) : RuntimeException(message)
class ConcertNotFoundException(message: String) : RuntimeException(message)
class InsufficientPointException(message: String) : RuntimeException(message)
class InvalidTokenStatusException(message: String) : RuntimeException(message)
class SeatAlreadyBookedException(message: String) : RuntimeException(message)
class InvalidTokenException(message: String) : RuntimeException(message)
class ConcertSoldOutException(message: String) : RuntimeException(message)
class ConcertDateExpiredException(message: String) : RuntimeException(message)
class InvalidateAmountException(message: String) : RuntimeException(message)
class PaymentAlreadyProcessedException(message: String) : RuntimeException(message)
class ReservationExpiredException(message: String) : RuntimeException(message)
class ReservationNotReservedException(message: String) : RuntimeException(message)


@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(ex: UserNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.error("User not found: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.NOT_FOUND.value(),
            data = null,
            message = ex.message ?: "User not found"
        )

        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(QueueTokenNotFoundException::class)
    fun handleQueueTokenNotFoundException(ex: QueueTokenNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.error("Queue token not found: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.NOT_FOUND.value(),
            data = null,
            message = ex.message ?: "Queue token not found"
        )

        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ConcertNotFoundException::class)
    fun handleConcertNotFoundException(ex: ConcertNotFoundException): ResponseEntity<ApiResponse<Any>> {
        log.error("Concert not found: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.NOT_FOUND.value(),
            data = null,
            message = ex.message ?: "Concert not found"
        )

        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(InsufficientPointException::class)
    fun handleInsufficientPointException(ex: InsufficientPointException): ResponseEntity<ApiResponse<Any>> {
        log.error("Insufficient point: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.BAD_REQUEST.value(),
            data = null,
            message = ex.message ?: "Insufficient point"
        )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidTokenStatusException::class)
    fun handleInvalidTokenStatusException(ex: InvalidTokenStatusException): ResponseEntity<ApiResponse<Any>> {
        log.error("Invalid token status: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.UNAUTHORIZED.value(),
            data = null,
            message = ex.message ?: "Invalid token status"
        )

        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException): ResponseEntity<ApiResponse<Any>> {
        log.error("Invalid token: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.UNAUTHORIZED.value(),
            data = null,
            message = ex.message ?: "Invalid token"
        )

        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(SeatAlreadyBookedException::class)
    fun handleSeatAlreadyBookedException(ex: SeatAlreadyBookedException): ResponseEntity<ApiResponse<Any>> {
        log.error("Seat already booked: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.CONFLICT.value(),
            data = null,
            message = ex.message ?: "Seat already booked"
        )

        return ResponseEntity(response, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ConcertSoldOutException::class)
    fun handleConcertSoldOutException(ex: ConcertSoldOutException): ResponseEntity<ApiResponse<Any>> {
        log.error("Concert sold out: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.GONE.value(),
            data = null,
            message = ex.message ?: "Concert is sold out"
        )

        return ResponseEntity(response, HttpStatus.GONE)
    }

    @ExceptionHandler(ConcertDateExpiredException::class)
    fun handleConcertDateExpiredException(ex: ConcertDateExpiredException): ResponseEntity<ApiResponse<Any>> {
        log.error("Concert date expired: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.GONE.value(),
            data = null,
            message = ex.message ?: "Concert date has expired"
        )

        return ResponseEntity(response, HttpStatus.GONE)
    }

    @ExceptionHandler(InvalidateAmountException::class)
    fun handleInvalidateAmountException(ex: InvalidateAmountException): ResponseEntity<ApiResponse<Any>> {
        log.error("Invalid amount: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.BAD_REQUEST.value(),
            data = null,
            message = ex.message ?: "Invalid amount"
        )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(PaymentAlreadyProcessedException::class)
    fun handlePaymentAlreadyProcessedException(ex: PaymentAlreadyProcessedException): ResponseEntity<ApiResponse<Any>> {
        log.error("Payment already processed: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.CONFLICT.value(),
            data = null,
            message = ex.message ?: "Payment already processed"
        )

        return ResponseEntity(response, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ReservationExpiredException::class)
    fun handleReservationExpiredException(ex: ReservationExpiredException): ResponseEntity<ApiResponse<Any>> {
        log.error("Reservation expired: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.GONE.value(),
            data = null,
            message = ex.message ?: "Reservation has expired"
        )

        return ResponseEntity(response, HttpStatus.GONE)
    }

    @ExceptionHandler(ReservationNotReservedException::class)
    fun handleReservationNotReservedException(ex: ReservationNotReservedException): ResponseEntity<ApiResponse<Any>> {
        log.error("Reservation not in reserved status: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.BAD_REQUEST.value(),
            data = null,
            message = ex.message ?: "Reservation is not in reserved status"
        )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Any>> {
        log.error("Invalid argument: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.BAD_REQUEST.value(),
            data = null,
            message = ex.message ?: "Invalid argument"
        )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ApiResponse<Any>> {
        log.error("Invalid state: ${ex.message}")

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.BAD_REQUEST.value(),
            data = null,
            message = ex.message ?: "Invalid state"
        )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Any>> {
        log.error("Unexpected error occurred: ${ex.message}", ex)

        val response = ApiResponse<Any>(
            success = false,
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            data = null,
            message = "An unexpected error occurred"
        )

        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}