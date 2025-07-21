package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user

import kr.hhplus.be.server.domain.users.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class MockUserRepository {
    private val log = LoggerFactory.getLogger(MockUserRepository::class.java)
    private val users = ConcurrentHashMap<String, User>()

    fun save(user: User): User {
        val isNewUser = !users.containsKey(user.userId)

        users[user.userId] = user

        if (isNewUser) {
            log.info("Inserted new User: ${user.userId}, Point: ${user.availablePoint}")
        } else {
            log.info("Updated User: ${user.userId}, Point: ${user.availablePoint}")
        }

        return user
    }

    fun findByUserId(id: String): User? {
        return users[id]
    }

    fun updatePoint(userId: String, point: Int): User {
        val user = users[userId]
            ?: throw IllegalArgumentException("User not found: $userId")

        when {
            point > 0 -> {
                user.chargePoint(point)
            }
            point < 0 -> {
                user.usePoint(-point)
            }
            else -> {
            }
        }

        users[userId] = user
        return user
    }
}