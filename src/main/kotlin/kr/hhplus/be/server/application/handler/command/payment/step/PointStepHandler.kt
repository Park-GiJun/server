package kr.hhplus.be.server.application.handler.command.payment.step

import kr.hhplus.be.server.application.dto.event.point.PointDeductedEvent
import kr.hhplus.be.server.application.dto.event.point.PointRefundedEvent
import kr.hhplus.be.server.application.dto.payment.command.PointDeductCommand
import kr.hhplus.be.server.application.dto.payment.command.PointRefundCommand
import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.domain.saga.exception.*
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.users.UserDomainService
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.domain.users.exception.InsufficientPointException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class PointStepHandler(
    private val userRepository: UserRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val userDomainService = UserDomainService()

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: PointDeductCommand) {
        try {
            val user = userRepository.findByUserIdWithLock(command.userId)
                ?: throw UserNotFoundException(command.userId)

            userDomainService.validateSufficientBalance(user, command.amount)

            user.usePoint(command.amount)
            userRepository.save(user)

            val history = PointHistory(
                userId = command.userId,
                pointHistoryAmount = command.amount,
                pointHistoryType = "PAYMENT",
                description = command.sagaId,
                pointHistoryId = 0
            )
            pointHistoryRepository.save(history)

            eventPublisher.publishEvent(
                PointDeductedEvent(
                    sagaId = command.sagaId,
                    userId = command.userId,
                    amount = command.amount,
                    transactionId = UUID.randomUUID().toString()
                )
            )

        } catch (e: UserNotFoundException) {
            throw PointDeductionSagaException(
                sagaId = command.sagaId,
                userId = command.userId,
                amount = command.amount,
                reason = "User not found",
                cause = e
            )
        } catch (e: InsufficientPointException) {
            throw PointDeductionSagaException(
                sagaId = command.sagaId,
                userId = command.userId,
                amount = command.amount,
                reason = "Insufficient points",
                cause = e
            )
        } catch (e: Exception) {
            throw PointDeductionSagaException(
                sagaId = command.sagaId,
                userId = command.userId,
                amount = command.amount,
                reason = "Unexpected error: ${e.message}",
                cause = e
            )
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: PointRefundCommand) {
        try {
            val user = userRepository.findByUserIdWithLock(command.userId)
                ?: throw UserNotFoundException(command.userId)

            user.chargePoint(command.amount)
            userRepository.save(user)

            val history = PointHistory(
                userId = command.userId,
                pointHistoryAmount = command.amount,
                pointHistoryType = "REFUND",
                description = command.sagaId,
                pointHistoryId = 0
            )
            pointHistoryRepository.save(history)

            eventPublisher.publishEvent(
                PointRefundedEvent(
                    sagaId = command.sagaId,
                    userId = command.userId,
                    amount = command.amount
                )
            )

        } catch (e: Exception) {
            throw CompensationSagaException(
                sagaId = command.sagaId,
                step = PaymentSagaState.POINT_REFUNDING,
                reason = "Point refund failed: ${e.message}",
                cause = e
            )
        }
    }
}