package kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.entity.PaymentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, Long> {
    fun findByPaymentId(paymentId: Long): PaymentJpaEntity?
    fun findByReservationId(reservationId: Long): PaymentJpaEntity?
    fun findByUserId(userId: String): List<PaymentJpaEntity>?
}