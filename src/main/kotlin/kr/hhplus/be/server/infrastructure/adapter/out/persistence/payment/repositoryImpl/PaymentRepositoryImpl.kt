package kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.repositoryImpl

import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.jpa.PaymentJpaRepository
import org.springframework.stereotype.Component

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        return PersistenceMapper.toPaymentEntity(payment)
            .let { paymentJpaRepository.save(it) }
            .let { PersistenceMapper.toPaymentDomain(it) }
    }

    override fun findByPaymentId(paymentId: Long): Payment? {
        return paymentJpaRepository.findByPaymentId(paymentId)
            ?.let { PersistenceMapper.toPaymentDomain(it) }
    }

    override fun findByReservationId(reservationId: Long): Payment? {
        return paymentJpaRepository.findByReservationId(reservationId)
            ?.let { PersistenceMapper.toPaymentDomain(it) }
    }

    override fun findByUserId(userId: String): List<Payment> {
        return paymentJpaRepository.findByUserId(userId)
            ?.map { PersistenceMapper.toPaymentDomain(it) }
            ?: emptyList()
    }
}