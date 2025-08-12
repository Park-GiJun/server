package kr.hhplus.be.server.application.dto.lock

enum class LockType {
    TEMP_RESERVATION_SEAT,
    TEMP_RESERVATION_PROCESS,
    PAYMENT_USER,
    POINT_CHARGE,
    SEAT_STATUS,
    QUEUE_ACTIVATION
}