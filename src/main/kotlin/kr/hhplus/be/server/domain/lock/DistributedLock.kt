package kr.hhplus.be.server.domain.lock

data class DistributedLock(
    val key: String,
    val type: DistributedLockType,
    val waitTime: Long = type.waitTime,
    val leaseTime: Long = type.leaseTime
)