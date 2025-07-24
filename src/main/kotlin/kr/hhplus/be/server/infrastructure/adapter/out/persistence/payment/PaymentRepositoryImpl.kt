package kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment

import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.mock.MockPaymentRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val mockPaymentRepository: MockPaymentRepository
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        return mockPaymentRepository.save(payment)
    }

    override fun findByPaymentId(paymentId: Long): Payment? {
        return mockPaymentRepository.findByPaymentId(paymentId)
    }

    override fun findByReservationId(reservationId: Long): Payment? {
        return mockPaymentRepository.findByReservationId(reservationId)
    }

    override fun findByUserId(userId: String): List<Payment> {
        return mockPaymentRepository.findByUserId(userId)
    }
}