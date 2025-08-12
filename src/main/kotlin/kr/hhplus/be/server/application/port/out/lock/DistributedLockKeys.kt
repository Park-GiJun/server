package kr.hhplus.be.server.application.port.out.lock

import kr.hhplus.be.server.application.dto.lock.LockType

/**
 * 분산락 키 생성 유틸리티 (애플리케이션 계층)
 * - 비즈니스 도메인과 락 타입을 연결
 */
object DistributedLockKeys {

    fun tempReservationSeat(concertSeatId: Long): Pair<String, LockType> =
        "lock:temp_reservation:seat:$concertSeatId" to LockType.TEMP_RESERVATION_SEAT

    fun tempReservationProcess(tempReservationId: Long): Pair<String, LockType> =
        "lock:temp_reservation:process:$tempReservationId" to LockType.TEMP_RESERVATION_PROCESS

    fun paymentUser(userId: String): Pair<String, LockType> =
        "lock:payment:user:$userId" to LockType.PAYMENT_USER

    fun pointCharge(userId: String): Pair<String, LockType> =
        "lock:point:charge:$userId" to LockType.POINT_CHARGE

    fun seatStatus(concertSeatId: Long): Pair<String, LockType> =
        "lock:seat:status:$concertSeatId" to LockType.SEAT_STATUS

    fun queueActivation(concertId: Long): Pair<String, LockType> =
        "lock:queue:activation:$concertId" to LockType.QUEUE_ACTIVATION
}