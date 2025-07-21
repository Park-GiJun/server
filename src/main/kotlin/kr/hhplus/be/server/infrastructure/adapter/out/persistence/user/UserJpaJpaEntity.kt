package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user

import jakarta.persistence.*
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_id", columnList = "user_id"),
    ]
)
class UserJpaJpaEntity(
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

    fun updateFromDomain(domain: User) {
        this.totalPoint = domain.totalPoint
        this.availablePoint = domain.availablePoint
        this.usedPoint = domain.usedPoint
        if (domain.isDeleted && !this.isDeleted) {
            this.delete()
        } else if (!domain.isDeleted && this.isDeleted) {
            this.restore()
        }
    }

    companion object {
        fun fromDomain(domain: User): UserJpaJpaEntity {
            val entity = UserJpaJpaEntity(
                userId = domain.userId,
                userName = domain.userName,
                totalPoint = domain.totalPoint,
                availablePoint = domain.availablePoint,
                usedPoint = domain.usedPoint
            )

            if (domain.isDeleted) {
                entity.delete()
            }

            return entity
        }
    }
}