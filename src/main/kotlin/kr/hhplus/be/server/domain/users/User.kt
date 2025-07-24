package kr.hhplus.be.server.domain.users

import java.time.LocalDateTime

class User(
    val userId: String,
    val userName: String,
    var totalPoint: Int = 0,
    var availablePoint: Int = 0,
    var usedPoint: Int = 0,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
) {

    fun chargePoint(amount: Int): User {
        require(amount > 0) { "충전 금액은 0보다 커야 합니다" }
        require(!isDeleted) { "삭제된 사용자는 포인트를 충전할 수 없습니다" }

        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint + amount,
            availablePoint = this.availablePoint + amount,
            usedPoint = this.usedPoint,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    fun usePoint(amount: Int): User {
        require(amount > 0) { "사용 금액은 0보다 커야 합니다" }
        require(!isDeleted) { "삭제된 사용자는 포인트를 사용할 수 없습니다" }
        require(availablePoint >= amount) { "잔액이 부족합니다. 필요: $amount, 사용가능: $availablePoint" }

        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint,
            availablePoint = this.availablePoint - amount,
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