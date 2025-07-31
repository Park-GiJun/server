package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "USERX0", columnList = "user_id"),
    ]
)
class UserJpaEntity(
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

    fun toDomain(): User {
        return User(
            userId = this.userId,
            userName = this.userName,
            totalPoint = this.totalPoint,
            availablePoint = this.availablePoint,
            usedPoint = this.usedPoint,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }
}