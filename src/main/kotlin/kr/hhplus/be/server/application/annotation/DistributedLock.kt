package kr.hhplus.be.server.application.annotation

import kr.hhplus.be.server.domain.lock.DistributedLockType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val type: DistributedLockType,
    val key: String,
    val waitTime: Long = -1L,
    val leaseTime: Long = -1L
)