package kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
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
    var usedPoint: Int = 0,

    @Version
    var version: Long = 0
) : BaseEntity()