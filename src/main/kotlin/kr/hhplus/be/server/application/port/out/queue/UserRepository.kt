package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.users.User

interface UserRepository {
    fun findByUserId(userId: String): User?
}
