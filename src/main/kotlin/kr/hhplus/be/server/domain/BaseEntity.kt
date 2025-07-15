package kr.hhplus.be.server.domain

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        private set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        private set

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
        private set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        private set

    fun delete() {
        isDeleted = true
        deletedAt = LocalDateTime.now()
    }

    fun restore() {
        isDeleted = false
        deletedAt = null
    }
}