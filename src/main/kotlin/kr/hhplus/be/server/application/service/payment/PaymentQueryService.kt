package kr.hhplus.be.server.application.service.payment

import kr.hhplus.be.server.application.dto.payment.GetPaymentCommand
import kr.hhplus.be.server.application.dto.payment.GetUserPaymentsCommand
import kr.hhplus.be.server.application.dto.payment.PaymentResult
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult
import kr.hhplus.be.server.application.mapper.PaymentMapper
import kr.hhplus.be.server.application.port.`in`.payment.GetPaymentUseCase
import kr.hhplus.be.server.application.port.`in`.payment.GetUserPaymentsUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.application.port.out.payment.PaymentRepository
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PaymentQueryService(
    private val paymentRepository: PaymentRepository,
    private val validateTokenUseCase: ValidateQueueTokenUseCase
) : GetPaymentUseCase, GetUserPaymentsUseCase {

    private val log = LoggerFactory.getLogger(PaymentQueryService::class.java)
    private val paymentDomainService = PaymentDomainService()

    override fun getPayment(command: GetPaymentCommand): PaymentResult {
        log.info("결제 내역 조회: paymentId=${command.paymentId}")

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )
        val token = createQueueTokenFromResult(tokenResult)

        val payment = paymentRepository.findByPaymentId(command.paymentId)
        paymentDomainService.validatePaymentExists(payment, command.paymentId)

        paymentDomainService.validatePaymentAccess(payment!!, token)

        log.info("결제 내역 조회 완료: userId=${token.userId}")
        return PaymentMapper.toResult(payment, "Payment details retrieved")
    }

    override fun getUserPayments(command: GetUserPaymentsCommand): List<PaymentResult> {
        log.info("사용자 결제 목록 조회: userId=${command.userId}")

        val tokenResult = validateTokenUseCase.validateActiveToken(
            ValidateQueueTokenCommand(command.tokenId)
        )
        val token = createQueueTokenFromResult(tokenResult)

        paymentDomainService.validateUserPaymentAccess(token, command.userId)

        val payments = paymentRepository.findByUserId(command.userId)

        log.info("사용자 결제 목록 조회 완료: ${payments.size}건")
        return PaymentMapper.toResults(payments)
    }

    private fun createQueueTokenFromResult(tokenResult: ValidateQueueTokenResult): QueueToken {
        return QueueToken(
            queueTokenId = tokenResult.tokenId,
            userId = tokenResult.userId,
            concertId = tokenResult.concertId,
            tokenStatus = QueueTokenStatus.COMPLETED,
            createdAt = LocalDateTime.now(),
            enteredAt = LocalDateTime.now()
        )
    }
}