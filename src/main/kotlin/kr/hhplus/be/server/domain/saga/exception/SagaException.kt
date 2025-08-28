import kr.hhplus.be.server.domain.common.exception.DomainException

/**
 * Saga 관련 예외 기본 클래스
 */
abstract class SagaException(
    message: String,
    val sagaId: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

