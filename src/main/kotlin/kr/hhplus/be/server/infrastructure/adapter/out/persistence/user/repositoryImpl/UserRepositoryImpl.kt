package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.repositoryImpl

import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.jpa.UserJpaRepository
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {

    override fun save(user: User): User {
        return PersistenceMapper.toUserEntity(user)
            .let { userJpaRepository.save(it) }
            .let { PersistenceMapper.toUserDomain(it) }
    }

    override fun findByUserId(userId: String): User? {
        return userJpaRepository.findByUserId(userId)
            ?.let { PersistenceMapper.toUserDomain(it) }
    }

    override fun findByUserIdWithLock(userId: String): User? {
        return userJpaRepository.findByUserIdWithLock(userId)
            ?.let { PersistenceMapper.toUserDomain(it) }
    }
}