package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

interface UserJpaRepository : CrudRepository<UserJpaEntity, String> {
    fun findByUserId(userId: String): UserJpaEntity?
}