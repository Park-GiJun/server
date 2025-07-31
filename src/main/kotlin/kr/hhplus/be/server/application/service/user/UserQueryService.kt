package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.GetUserUseCase
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.users.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserQueryService(
    private val userRepository: UserRepository
) : GetUserUseCase {

    private val userDomainService = UserDomainService()

    override fun getUser(command: GetUserCommand): UserResult {
        val user = userRepository.findByUserId(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        return UserMapper.toResult(user!!)
    }
}