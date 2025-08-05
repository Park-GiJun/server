package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.user.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.users.UserDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserCommandService(
    private val userRepository: UserRepository
) : ChargeUserPointUseCase, UseUserPointUseCase {

    private val log = LoggerFactory.getLogger(UserCommandService::class.java)
    private val userDomainService = UserDomainService()

    override fun chargeUserPoint(command: ChargeUserPointCommand): UserResult {
        log.info("포인트 충전: userId=${command.userId}, amount=${command.amount}")

        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        val updatedUser = userDomainService.chargeUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        log.info("포인트 충전 완료: 잔액=${savedUser.availablePoint}")
        return UserMapper.toResult(savedUser)
    }

    override fun useUserPoint(command: UseUserPointCommand): UserResult {
        log.info("포인트 사용: userId=${command.userId}, amount=${command.amount}")

        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        val updatedUser = userDomainService.useUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        log.info("포인트 사용 완료: 잔액=${savedUser.availablePoint}")
        return UserMapper.toResult(savedUser)
    }
}