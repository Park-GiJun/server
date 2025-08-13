package kr.hhplus.be.server.application.port.out.lock

interface DistributedLockPort {
    fun <T> executeWithLock(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        action: () -> T
    ): T
}