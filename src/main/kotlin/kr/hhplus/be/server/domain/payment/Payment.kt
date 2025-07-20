package kr.hhplus.be.server.domain.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity
import java.time.LocalDateTime
import javax.annotation.processing.Generated

@Table(name = "payments")
@Entity
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val paymentId: Long,

    @Column(name = "reservation_id")
    val reservationId: Long,

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "total_amount")
    val totalAmount: Int,

    @Column(name = "discount_amount")
    val discountAmount: Int,

    @Column(name = "actual_amount")
    val actualAmount: Int,

    @Column(name = "payment_at")
    val paymentAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_cancel")
    val isCancel: Boolean,

    @Column(name = "is_refund")
    val isRefund: Boolean,

    @Column(name = "cancel_at")
    val cancelAt: LocalDateTime?
) : BaseEntity()