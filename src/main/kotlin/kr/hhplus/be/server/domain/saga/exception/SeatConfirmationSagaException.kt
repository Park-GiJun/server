package kr.hhplus.be.server.domain.saga.exception

import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * 좌석 확정 실패
 */
class SeatConfirmationSagaException(
    sagaId: String,
    seatId: Long,
    reason: String,
    cause: Throwable? = null
) : SagaStepException(
    message = "Seat confirmation failed for seat $seatId: $reason",
    sagaId = sagaId,
    stepState = PaymentSagaState.SEAT_CONFIRMING,
    shouldCompensate = true,
    cause = cause
) {
    val seatId = seatId
    val reason = reason
}