package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.jpa

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserJpaRepository : CrudRepository<UserJpaEntity, String> {
    fun findByUserId(userId: String): UserJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserJpaEntity u WHERE u.userId = :userId")
    fun findByUserIdWithLock(@Param("userId") userId: String): UserJpaEntity?
}