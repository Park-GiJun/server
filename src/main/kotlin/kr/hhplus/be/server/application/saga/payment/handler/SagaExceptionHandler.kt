package kr.hhplus.be.server.application.saga.payment.handler

import kr.hhplus.be.server.application.dto.concert.command.SeatReleaseCommand
import kr.hhplus.be.server.application.dto.event.saga.payment.PaymentSagaFailedEvent
import kr.hhplus.be.server.application.dto.payment.command.PointRefundCommand
import kr.hhplus.be.server.application.dto.reservation.command.ReservationCancelCommand
import kr.hhplus.be.server.application.port.out.saga.SagaRepository
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import kr.hhplus.be.server.domain.saga.exception.CompensationSagaException
import kr.hhplus.be.server.domain.saga.exception.PaymentCreationSagaException
import kr.hhplus.be.server.domain.saga.exception.PointDeductionSagaException
import kr.hhplus.be.server.domain.saga.exception.ReservationConfirmationSagaException
import kr.hhplus.be.server.domain.saga.exception.SeatConfirmationSagaException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class SagaExceptionHandler(
    private val sagaRepository: SagaRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(SagaExceptionHandler::class.java)

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(ex: PointDeductionSagaException) {
        log.error("Handling point deduction failure: ${ex.sagaId}")

        val context = sagaRepository.findById(ex.sagaId!!)
            ?: throw IllegalStateException("Saga not found: ${ex.sagaId}")

        context.state = PaymentSagaState.FAILED
        context.failureReason = ex.reason
        context.completedAt = LocalDateTime.now()
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            PaymentSagaFailedEvent(
                sagaId = ex.sagaId,
                userId = context.userId,
                failureReason = ex.reason
            )
        )

        sagaRepository.delete(ex.sagaId)
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(ex: SeatConfirmationSagaException) {
        log.error("Handling seat confirmation failure: ${ex.sagaId}")

        val context = sagaRepository.findById(ex.sagaId!!)
            ?: throw IllegalStateException("Saga not found: ${ex.sagaId}")

        context.state = PaymentSagaState.COMPENSATING
        context.failureReason = ex.reason
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            PointRefundCommand(
                sagaId = ex.sagaId,
                userId = context.userId,
                amount = context.pointsToUse + context.totalAmount
            )
        )
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(ex: ReservationConfirmationSagaException) {
        log.error("Handling reservation confirmation failure: ${ex.sagaId}")

        val context = sagaRepository.findById(ex.sagaId!!)
            ?: throw IllegalStateException("Saga not found: ${ex.sagaId}")

        context.state = PaymentSagaState.COMPENSATING
        context.failureReason = ex.reason
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            SeatReleaseCommand(
                sagaId = ex.sagaId,
                seatId = context.seatId
            )
        )
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(ex: PaymentCreationSagaException) {
        log.error("Handling payment creation failure: ${ex.sagaId}")

        val context = sagaRepository.findById(ex.sagaId!!)
            ?: throw IllegalStateException("Saga not found: ${ex.sagaId}")

        context.state = PaymentSagaState.COMPENSATING
        context.failureReason = ex.reason
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            ReservationCancelCommand(
                sagaId = ex.sagaId,
                userId = context.userId,
                reservationId = context.actualReservationId ?: context.reservationId
            )
        )
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(ex: CompensationSagaException) {
        log.error("Compensation failed for saga: ${ex.sagaId} - ${ex.reason}")

        val context = sagaRepository.findById(ex.sagaId!!)
            ?: throw IllegalStateException("Saga not found: ${ex.sagaId}")

        context.state = PaymentSagaState.FAILED
        context.failureReason = "Compensation failed: ${ex.reason}"
        context.completedAt = LocalDateTime.now()
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            PaymentSagaFailedEvent(
                sagaId = ex.sagaId,
                userId = context.userId,
                failureReason = context.failureReason!!
            )
        )
        sagaRepository.delete(ex.sagaId)
    }
}