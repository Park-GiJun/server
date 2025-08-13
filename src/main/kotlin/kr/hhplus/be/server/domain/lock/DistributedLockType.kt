package kr.hhplus.be.server.domain.lock

enum class DistributedLockType (
    val keyPrefix: String,
    val waitTime: Long = 10L,
    val leaseTime: Long = 30L
) {
    TEMP_RESERVATION_SEAT("lock:temp_reservation:seat", 5L, 10L),
    PAYMENT_USER("lock:payment:user", 10L, 30L),
    SEAT_STATUS("lock:seat:status", 3L, 10L),
}