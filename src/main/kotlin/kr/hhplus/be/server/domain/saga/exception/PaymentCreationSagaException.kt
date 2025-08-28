package kr.hhplus.be.server.domain.saga.exception

import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * 결제 생성 실패
 */
class PaymentCreationSagaException(
    sagaId: String,
    reason: String,
    cause: Throwable? = null
) : SagaStepException(
    message = "Payment creation failed: $reason",
    sagaId = sagaId,
    stepState = PaymentSagaState.PAYMENT_CREATING,
    shouldCompensate = true,
    cause = cause
) {
    val reason = reason
}