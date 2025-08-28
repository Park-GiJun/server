package kr.hhplus.be.server.domain.payment

import java.time.LocalDateTime

class Payment(
    val paymentId: Long,
    val reservationId: Long,
    val userId: String,
    val totalAmount: Int,
    val discountAmount: Int,
    val actualAmount: Int,
    val paymentAt: LocalDateTime = LocalDateTime.now(),
    val isCancel: Boolean = false,
    val isRefund: Boolean = false,
    val cancelAt: LocalDateTime? = null,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)