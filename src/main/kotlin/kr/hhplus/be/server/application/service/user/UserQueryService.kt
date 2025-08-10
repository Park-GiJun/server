package kr.hhplus.be.server.application.service.user

import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UserResult
import kr.hhplus.be.server.application.mapper.UserMapper
import kr.hhplus.be.server.application.port.`in`.user.GetUserUseCase
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.users.UserDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserQueryService(
    private val userRepository: UserRepository
) : GetUserUseCase {

    private val log = LoggerFactory.getLogger(UserQueryService::class.java)
    private val userDomainService = UserDomainService()

    override fun getUser(command: GetUserCommand): UserResult {
        log.info("사용자 정보 조회: userId=${command.userId}")

        val user = userRepository.findByUserId(command.userId)
        userDomainService.validateUserExists(user, command.userId)

        log.info("사용자 정보 조회 완료: 잔액=${user!!.availablePoint}")
        return UserMapper.toResult(user)
    }
}