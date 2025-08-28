package kr.hhplus.be.server.domain.saga.exception

import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * 예약 확정 실패
 */
class ReservationConfirmationSagaException(
    sagaId: String,
    reservationId: Long,
    reason: String,
    cause: Throwable? = null
) : SagaStepException(
    message = "Reservation confirmation failed for reservation $reservationId: $reason",
    sagaId = sagaId,
    stepState = PaymentSagaState.RESERVATION_CONFIRMING,
    shouldCompensate = true,
    cause = cause
) {
    val reservationId = reservationId
    val reason = reason
}