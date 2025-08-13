package kr.hhplus.be.server.domain.lock

enum class DistributedLockType (
    val waitTime: Long = 10L,
    val leaseTime: Long = 30L
) {
    TEMP_RESERVATION_SEAT(5L, 10L),
    TEMP_RESERVATION_PROCESS(5L, 30L),
    PAYMENT_USER(10L, 30L),
}