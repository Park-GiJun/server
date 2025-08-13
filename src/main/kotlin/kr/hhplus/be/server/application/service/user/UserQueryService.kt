package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.user.GetUserUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserQueryService(
    private val userRepository: UserRepository,
    private val userDomainService: UserDomainService
) : GetUserUseCase {

    private val log = LoggerFactory.getLogger(UserQueryService::class.java)


    override fun getUser(command: GetUserCommand): UserResult {
        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)
        userDomainService.validateUserExists(user, command.userId)
        return UserMapper.toResult(user)
    }
}