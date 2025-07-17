package kr.hhplus.be.server.domain.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity

@Entity
@Table(name = "users",
    indexes = [
        Index(name ="idex_user", columnList = "user_id"),
    ])
class User(
    @Id
    @Column(name = "user_id")
    val userId: String,

    @Column(name = "user_name", nullable = false, length = 100)
    val userName: String,

    @Column(name = "total_point", nullable = false)
    var totalPoint: Int = 0,

    @Column(name = "available_point", nullable = false)
    var availablePoint: Int = 0,

    @Column(name = "used_point", nullable = false)
    var usedPoint: Int = 0
) : BaseEntity() {
    fun chargePoint(amount: Int) {
        require(amount > 0) { "충전 금액은 0보다 커야 합니다" }
        require(!isDeleted) { "삭제된 사용자는 포인트를 충전할 수 없습니다" }

        totalPoint += amount
        availablePoint += amount
    }

    fun usePoint(amount: Int) {
        require(amount > 0) { "사용 금액은 0보다 커야 합니다" }
        require(!isDeleted) { "삭제된 사용자는 포인트를 사용할 수 없습니다" }
        require(availablePoint >= amount) { "잔액이 부족합니다" }

        availablePoint -= amount
        usedPoint += amount
    }
}