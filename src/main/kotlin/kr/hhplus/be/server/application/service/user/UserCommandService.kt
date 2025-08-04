package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.user.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.user.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.users.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserCommandService(
    private val userRepository: UserRepository
) : ChargeUserPointUseCase, UseUserPointUseCase {

    private val userDomainService = UserDomainService()

    override fun chargeUserPoint(command: ChargeUserPointCommand): UserResult {
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        val updatedUser = userDomainService.chargeUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        return UserMapper.toResult(savedUser)
    }

    override fun useUserPoint(command: UseUserPointCommand): UserResult {
        val user = userRepository.findByUserIdWithLock(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        val updatedUser = userDomainService.useUserPoint(user!!, command.amount)
        val savedUser = userRepository.save(updatedUser)

        return UserMapper.toResult(savedUser)
    }
}