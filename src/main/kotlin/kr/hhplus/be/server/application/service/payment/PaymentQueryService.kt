package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.GetPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.payment.GetUserPaymentsUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class  PaymentQueryService(
    private val paymentRepository: PaymentRepository,
    private val validateTokenUseCase: ValidateTokenUseCase
) : GetPaymentUseCase, GetUserPaymentsUseCase {

    private val paymentDomainService = PaymentDomainService()

    override fun getPayment(command: GetPaymentCommand): PaymentResult {
        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )
        val token = createQueueTokenFromResult(tokenResult)

        val payment = paymentRepository.findByPaymentId(command.paymentId)
        paymentDomainService.validatePaymentExists(payment, command.paymentId)

        paymentDomainService.validatePaymentAccess(payment!!, token)

        return PaymentMapper.toResult(payment, "Payment details retrieved")
    }

    override fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult> {
        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateTokenCommand(command.tokenId)
        )
        val token = createQueueTokenFromResult(tokenResult)

        paymentDomainService.validateUserPaymentAccess(token, command.userId)

        val payments = paymentRepository.findByUserId(command.userId)
        return PaymentMapper.toResults(payments)
    }

    private fun createQueueTokenFromResult(tokenResult: ValidateTokenResult): QueueToken {
        return QueueToken(
            queueTokenId = tokenResult.tokenId,
            userId = tokenResult.userId,
            concertId = tokenResult.concertId,
            tokenStatus = QueueTokenStatus.ACTIVE
        )
    }
}