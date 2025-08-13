package kr.hhplus.be.server.application.service.lock

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class DistributedLockAspect(
    private val distributedLockPort: DistributedLockPort
) {

    @Around("@annotation(distributedLock)")
    fun executeWithDistributedLock(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock
    ): Any? {
    }
}