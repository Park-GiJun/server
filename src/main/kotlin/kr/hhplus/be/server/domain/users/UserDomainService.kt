package kr.hhplus.be.server.domain.users

import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import kr.hhplus.be.server.domain.users.exception.InvalidPointAmountException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException

class UserDomainService {

    fun validateUserExists(user: User?, userId: String) {
        if (user == null) {
            throw UserNotFoundException(userId)
        }
    }

    fun validatePointAmount(amount: Int) {
        if (amount <= 0) {
            throw InvalidPointAmountException(amount)
        }
    }

    fun chargeUserPoint(user: User, amount: Int): User {
        validatePointAmount(amount)
        return user.chargePoint(amount)
    }

    fun useUserPoint(user: User, amount: Int): User {
        validatePointAmount(amount)
        return user.usePoint(amount)
    }

    fun validateSufficientBalance(user: User, requiredAmount: Int) {
        if (!user.hasEnoughPoint(requiredAmount)) {
            throw InsufficientPointException(
                requiredAmount,
                user.availablePoint
            )
        }
    }

}