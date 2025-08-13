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
@Transactional
class UserCommandService(
    private val userRepository: UserRepository,
    private val userDomainService: UserDomainService,
    private val pointHistoryRepository: PointHistoryRepository
) : ChargeUserPointUseCase, UseUserPointUseCase {
    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "lock:payment:user:#{#command.userId}",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun chargeUserPoint(command: ChargeUserPointCommand): UserResult {
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)
        val updatedUser = userDomainService.chargeUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)
        val pointHistory = PointHistory(
            pointHistoryId = 0L,
            userId = savedUser.userId,
            pointHistoryType = "CHARGED",
            pointHistoryAmount = command.amount,
            description = "Point charge via API"
        )
        pointHistoryRepository.save(pointHistory)
        return UserMapper.toResult(savedUser)
    }

    @DistributedLock(
        type = DistributedLockType.PAYMENT_USER,
        key = "lock:payment:user:#{#command.userId}",
        waitTime = 10L,
        leaseTime = 30L
    )
    override fun useUserPoint(command: UseUserPointCommand): UserResult {
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)
        val updatedUser = userDomainService.useUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)
        val pointHistory = PointHistory(
            pointHistoryId = 0L,
            userId = savedUser.userId,
            pointHistoryType = "USED",
            pointHistoryAmount = command.amount,
            description = "Point usage via API"
        )
        pointHistoryRepository.save(pointHistory)
        return UserMapper.toResult(savedUser)
    }
}