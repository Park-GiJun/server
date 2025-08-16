package kr.hhplus.be.server.domain.lock.exception

class LockAcquisitionException(
    lockKey: String,
    cause: Throwable? = null
) : RuntimeException("락 획득 실패: $lockKey", cause)