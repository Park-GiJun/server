package kr.hhplus.be.server.domain.saga.exception

import SagaException
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * 보상 트랜잭션 실패
 */
class CompensationSagaException(
    sagaId: String,
    step: PaymentSagaState,
    reason: String,
    cause: Throwable? = null
) : SagaException(
    message = "Compensation failed at step $step: $reason",
    sagaId = sagaId,
    cause = cause
) {
    val step = step
    val reason = reason
}