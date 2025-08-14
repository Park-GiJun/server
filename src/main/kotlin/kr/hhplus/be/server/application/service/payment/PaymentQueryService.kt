package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.GetPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.payment.GetUserPaymentsUseCase
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.payment.exception.PaymentNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentQueryService(
    private val paymentRepository: PaymentRepository,
    
) : GetPaymentUseCase, GetUserPaymentsUseCase {

    private val paymentDomainService = PaymentDomainService()
    override fun getPayment(command: GetPaymentCommand): PaymentResult {
        val payment = paymentRepository.findByPaymentId(command.paymentId)
            ?: throw PaymentNotFoundException(command.paymentId)
        paymentDomainService.validatePaymentExists(payment, command.paymentId)
        return PaymentMapper.toResult(payment, "Payment details retrieved")
    }

    override fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult> {
        val payments = paymentRepository.findByUserId(command.userId)
        return PaymentMapper.toResults(payments)
    }
}