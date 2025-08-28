package kr.hhplus.be.server.domain.saga.exception

import SagaException
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState

/**
 * Saga 단계별 실행 예외 - 보상 트랜잭션 필요
 */
abstract class SagaStepException(
    message: String,
    sagaId: String,
    val stepState: PaymentSagaState,
    val shouldCompensate: Boolean = true,
    cause: Throwable? = null
) : SagaException(message, sagaId, cause)