package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.annotation.DistributedLock
import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.user.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.domain.lock.DistributedLockType
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCommandService(
    private val userRepository: UserRepository,
    private val pointHistoryRepository: PointHistoryRepository
) : ChargeUserPointUseCase, UseUserPointUseCase {

    private val userDomainService = UserDomainService()
    private val log = LoggerFactory.getLogger(UserCommandService::class.java)

    /**
     * 포인트 충전
     * 실행 순서: 트랜잭션 시작 -> 분산락 획득 -> 비즈니스 로직 -> 락 해제 -> 트랜잭션 종료
     */
    @Transactional
    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "'lock:payment:user:' + #command.userId",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun chargeUserPoint(command: ChargeUserPointCommand): UserResult {
        log.info("포인트 충전 시작: userId=${command.userId}, amount=${command.amount}")

        // 1. 사용자 조회 (비관적 락)
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        // 2. 포인트 충전
        val updatedUser = userDomainService.chargeUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        // 3. 포인트 히스토리 저장
        val pointHistory = PointHistory(
            pointHistoryId = 0L,
            userId = savedUser.userId,
            pointHistoryType = "CHARGED",
            pointHistoryAmount = command.amount,
            description = "Point charge via API"
        )
        pointHistoryRepository.save(pointHistory)

        log.info("포인트 충전 완료: userId=${command.userId}, newBalance=${savedUser.availablePoint}")

        return UserMapper.toResult(savedUser)
    }

    /**
     * 포인트 사용
     * 실행 순서: 트랜잭션 시작 -> 분산락 획득 -> 비즈니스 로직 -> 락 해제 -> 트랜잭션 종료
     */
    @Transactional
    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "'lock:payment:user:' + #command.userId",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun useUserPoint(command: UseUserPointCommand): UserResult {
        log.info("포인트 사용 시작: userId=${command.userId}, amount=${command.amount}")

        // 1. 사용자 조회 (비관적 락)
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        // 2. 포인트 사용
        val updatedUser = userDomainService.useUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        // 3. 포인트 히스토리 저장
        val pointHistory = PointHistory(
            pointHistoryId = 0L,
            userId = savedUser.userId,
            pointHistoryType = "USED",
            pointHistoryAmount = command.amount,
            description = "Point usage via API"
        )
        pointHistoryRepository.save(pointHistory)

        log.info("포인트 사용 완료: userId=${command.userId}, remainingBalance=${savedUser.availablePoint}")

        return UserMapper.toResult(savedUser)
    }
}