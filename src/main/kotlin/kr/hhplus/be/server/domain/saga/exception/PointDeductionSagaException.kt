package kr.hhplus.be.server.domain.saga.exception

import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * 포인트 차감 실패
 */
class PointDeductionSagaException(
    sagaId: String,
    userId: String,
    amount: Int,
    reason: String,
    cause: Throwable? = null
) : SagaStepException(
    message = "Point deduction failed for user $userId, amount $amount: $reason",
    sagaId = sagaId,
    stepState = PaymentSagaState.POINT_DEDUCTING,
    shouldCompensate = false,
    cause = cause
) {
    val userId = userId
    val amount = amount
    val reason = reason
}