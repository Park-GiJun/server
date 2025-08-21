package kr.hhplus.be.server.infrastructure.config.aop

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * AOP 설정
 *
 * 실행 순서 보장:
 * 1. @Transactional (order = 1) - 트랜잭션 시작
 * 2. @DistributedLock (order = 2) - 분산 락 획득
 * 3. 비즈니스 로직 실행
 * 4. 분산 락 해제 (order = 2)
 * 5. 트랜잭션 커밋/롤백 (order = 1)
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement(order = 1)
class AopConfig