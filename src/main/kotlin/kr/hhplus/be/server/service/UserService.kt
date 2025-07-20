package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.exception.UserNotFoundException
import kr.hhplus.be.server.repository.mock.MockUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: MockUserRepository) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun getUser(id: String): User {
        return userRepository.findByUserId(id)
            ?: throw UserNotFoundException("User not found with id: $id")
    }
}