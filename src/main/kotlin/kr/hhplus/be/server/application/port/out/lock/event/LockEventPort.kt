package kr.hhplus.be.server.application.port.out.lock.event

import kr.hhplus.be.server.application.dto.lock.LockEvent
import kr.hhplus.be.server.application.dto.lock.LockType


interface LockEventPort {
    suspend fun publishLockEvent(event: LockEvent)
    suspend fun publishLockReleased(lockKey: String, lockType: LockType)
    suspend fun publishLockAcquired(lockKey: String, lockType: LockType, holderId: String)
}