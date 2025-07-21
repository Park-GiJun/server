package kr.hhplus.be.server.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.exception.InvalidateAmountException
import kr.hhplus.be.server.exception.UserNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.mock.MockUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class PointService(private val userRepository: MockUserRepository) {
    private val log = LoggerFactory.getLogger(PointService::class.java)

    fun getPoint(userId: String): User {
        return validateUser(userId)
    }

    fun chargePoint(userId: String, amount: Int): User {
        validateUser(userId)
        validatePoint(amount)

        return try {
            userRepository.updatePoint(userId, amount)
        } catch (e: Exception) {
            log.error("Failed to charge points for user $userId: ${e.message}")
            throw e
        }
    }

    fun usePoint(userId: String, amount: Int): User {
        validateUser(userId)
        validatePoint(amount)

        return try {
            userRepository.updatePoint(userId, -amount)
        } catch (e: Exception) {
            log.error("Failed to use points for user $userId: ${e.message}")
            throw e
        }
    }

    private fun validateUser(userId: String): User {
        return userRepository.findByUserId(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun validatePoint(point: Int) : Boolean{
        return if(point>0) true else throw InvalidateAmountException("Point must be Positive $point")
    }
}