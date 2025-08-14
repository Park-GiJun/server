package kr.hhplus.be.server.domain.lock.exception


class LockTimeoutException(
    lockKey: String,
    waitTime: Long
) : RuntimeException("락 획득 타임아웃: $lockKey (대기시간: ${waitTime}초)")