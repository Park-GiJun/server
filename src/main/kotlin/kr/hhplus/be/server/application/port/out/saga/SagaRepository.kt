package kr.hhplus.be.server.application.port.out.saga

import kr.hhplus.be.server.application.saga.payment.PaymentSagaContext

interface SagaRepository {
    fun save(context: PaymentSagaContext)
    fun findById(sagaId: String): PaymentSagaContext?
    fun delete(sagaId: String)
    fun getActiveSagaCount(): Long
    fun findAllActive(): List<PaymentSagaContext>
}
