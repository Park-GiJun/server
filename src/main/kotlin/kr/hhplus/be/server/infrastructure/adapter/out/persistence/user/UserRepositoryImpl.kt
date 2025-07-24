package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user

import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.mock.MockUserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val mockUserRepository: MockUserRepository
) : UserRepository {

    override fun findByUserId(userId: String): User? {
        return mockUserRepository.findByUserId(userId)
    }

    override fun save(user: User): User {
        return mockUserRepository.save(user)
    }
}