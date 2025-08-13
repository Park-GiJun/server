package kr.hhplus.be.server.application.aspect

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
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

        return distributedLockPort.executeWithLock(
            lockKey = lockKey,
            waitTime = distributedLock.waitTime,
            leaseTime = distributedLock.leaseTime
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