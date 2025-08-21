package kr.hhplus.be.server.application.aspect

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

/**
 * 분산 락 AOP
 *
 * 실행 순서: @Transactional(Order=1) -> @DistributedLock(Order=2) -> 비즈니스 로직
 * 이를 통해 트랜잭션 시작 -> 락 획득 -> 로직 실행 -> 락 해제 -> 트랜잭션 종료 순서가 보장됩니다.
 */
@Aspect
@Component
@Order(2) // @Transactional의 기본 Order는 Ordered.LOWEST_PRECEDENCE이므로, 2로 설정하여 트랜잭션 이후에 실행
class DistributedLockAspect(
    private val distributedLockPort: DistributedLockPort
) {
    private val parser = SpelExpressionParser()

    @Around("@annotation(distributedLock)")
    fun executeWithDistributedLock(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock
    ): Any? {
        val lockKey = parseLockKey(distributedLock.key, joinPoint)
        val waitTime = if (distributedLock.waitTime != -1L) distributedLock.waitTime else distributedLock.type.waitTime
        val leaseTime = if (distributedLock.leaseTime != -1L) distributedLock.leaseTime else distributedLock.type.leaseTime

        return distributedLockPort.executeWithLock(
            lockKey = lockKey,
            waitTime = waitTime,
            leaseTime = leaseTime
        ) {
            joinPoint.proceed()
        }
    }

    private fun parseLockKey(keyExpression: String, joinPoint: ProceedingJoinPoint): String {
        val context = StandardEvaluationContext()

        val parameterNames = joinPoint.signature.let { signature ->
            when (signature) {
                is MethodSignature -> signature.parameterNames
                else -> emptyArray()
            }
        }

        parameterNames.forEachIndexed { index, paramName ->
            context.setVariable(paramName, joinPoint.args[index])
        }

        return parser.parseExpression(keyExpression).getValue(context, String::class.java)
            ?: throw IllegalArgumentException("락 키 파싱 실패: $keyExpression")
    }
}