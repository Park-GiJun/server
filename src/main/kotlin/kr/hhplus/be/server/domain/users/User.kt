package kr.hhplus.be.server.domain.users

import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import kr.hhplus.be.server.domain.users.exception.InvalidPointAmountException
import java.time.LocalDateTime

class User(
    val userId: String,
    val userName: String,
    var totalPoint: Int = 0,
    var availablePoint: Int = 0,
    var usedPoint: Int = 0,
    var version: Long = 0,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
) {

    fun chargePoint(amount: Int): User {
        if (amount <= 0) throw InvalidPointAmountException(amount)
        require(!isDeleted) { "삭제된 사용자는 포인트를 충전할 수 없습니다" }

        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint + amount,
            availablePoint = this.availablePoint + amount,
            usedPoint = this.usedPoint,
            version = this.version,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun usePoint(amount: Int): User {
        if (amount <= 0) throw InvalidPointAmountException(amount)
        require(!isDeleted) { "삭제된 사용자는 포인트를 사용할 수 없습니다" }
        if (availablePoint < amount) {
            throw InsufficientPointException(amount, availablePoint)
        }

        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint,
            availablePoint = this.availablePoint - amount,
            version = this.version,
            usedPoint = this.usedPoint + amount,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun delete(): User {
        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint,
            availablePoint = this.availablePoint,
            usedPoint = this.usedPoint,
            version = this.version,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = true,
            deletedAt = LocalDateTime.now()
        )
    }

    fun hasEnoughPoint(amount: Int): Boolean {
        return availablePoint >= amount
    }
}