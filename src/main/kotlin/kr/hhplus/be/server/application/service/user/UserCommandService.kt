package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.ChargeUserPointUseCase
import kr.hhplus.be.server.application.port.`in`.GetUserUseCase
import kr.hhplus.be.server.application.port.`in`.UseUserPointUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.users.exception.InvalidPointAmountException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserCommandService(
    private val userRepository: UserRepository
) : GetUserUseCase, ChargeUserPointUseCase, UseUserPointUseCase {

    @Transactional(readOnly = true)
    override fun getUser(command: GetUserCommand): UserResult {
        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        return UserMapper.toResult(user)
    }

    override fun chargeUserPoint(command: ChargeUserPointCommand): UserResult {
        if (command.amount <= 0) {
            throw InvalidPointAmountException(command.amount)
        }

        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val updatedUser = user.chargePoint(command.amount)
        val savedUser = userRepository.save(updatedUser)

        return UserMapper.toResult(savedUser)
    }

    override fun useUserPoint(command: UseUserPointCommand): UserResult {
        if (command.amount <= 0) {
            throw InvalidPointAmountException(command.amount)
        }

        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val updatedUser = user.usePoint(command.amount)
        val savedUser = userRepository.save(updatedUser)

        return UserMapper.toResult(savedUser)
    }
}