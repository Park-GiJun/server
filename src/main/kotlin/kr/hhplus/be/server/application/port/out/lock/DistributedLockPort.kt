package kr.hhplus.be.server.application.port.out.lock

import kr.hhplus.be.server.application.dto.lock.LockType
import java.time.Duration

interface DistributedLockPort {
    fun tryLock(key: String, waitTime: Duration, leaseTime: Duration): Boolean
    fun unlock(key: String)
    fun <T> withLock(key: String, waitTime: Duration, leaseTime: Duration, action: () -> T): T
    fun <T> withLock(key: String, lockType: LockType, waitTime: Duration, leaseTime: Duration, action: () -> T): T
}
