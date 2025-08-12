package kr.hhplus.be.server.infrastructure.adapter.out.lock.exception

open class DistributedLockException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class DistributedLockAcquisitionException(message: String) : DistributedLockException(message)
