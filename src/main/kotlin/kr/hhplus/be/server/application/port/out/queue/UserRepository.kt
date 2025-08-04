package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.users.User

interface UserRepository {
    fun save(user: User): User
    fun findByUserId(userId: String): User?
    fun findByUserIdWithLock(userId: String): User?
}
