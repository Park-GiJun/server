package kr.hhplus.be.server.domain.lock

enum class DistributedLockType (
    val waitTime: Long = 0L,
    val leaseTime: Long = 30L
) {
    TEMP_RESERVATION_SEAT(0L, 10L),
    TEMP_RESERVATION_PROCESS(0L, 30L),
    PAYMENT_USER(0L, 30L),
}