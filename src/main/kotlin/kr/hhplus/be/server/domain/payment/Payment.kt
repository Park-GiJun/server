package kr.hhplus.be.server.domain.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity
import java.time.LocalDateTime

class Payment(
    val paymentId: Long,
    val reservationId: Long,
    val userId: String,
    val totalAmount: Int,
    val discountAmount: Int,
    val actualAmount: Int,
    val paymentAt: LocalDateTime = LocalDateTime.now(),
    val isCancel: Boolean,
    val isRefund: Boolean,
    val cancelAt: LocalDateTime?,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)