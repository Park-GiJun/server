package kr.hhplus.be.server.application.saga.payment

import kr.hhplus.be.server.application.dto.concert.command.SeatConfirmCommand
import kr.hhplus.be.server.application.dto.event.concert.SeatConfirmedEvent
import kr.hhplus.be.server.application.dto.event.concert.SeatReleasedEvent
import kr.hhplus.be.server.application.dto.event.payment.PaymentCreatedEvent
import kr.hhplus.be.server.application.dto.event.point.PointDeductedEvent
import kr.hhplus.be.server.application.dto.event.point.PointRefundedEvent
import kr.hhplus.be.server.application.dto.event.reservation.ReservationConfirmedEvent
import kr.hhplus.be.server.application.dto.event.saga.payment.PaymentInitiatedEvent
import kr.hhplus.be.server.application.dto.event.saga.payment.PaymentSagaCompletedEvent
import kr.hhplus.be.server.application.dto.event.saga.payment.PaymentSagaFailedEvent
import kr.hhplus.be.server.application.dto.payment.command.CreatePaymentCommand
import kr.hhplus.be.server.application.dto.payment.command.PointDeductCommand
import kr.hhplus.be.server.application.dto.payment.command.PointRefundCommand
import kr.hhplus.be.server.application.dto.reservation.command.ReservationConfirmCommand
import kr.hhplus.be.server.application.port.out.lock.DistributedLockPort
import kr.hhplus.be.server.application.port.out.saga.SagaRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentSagaOrchestrator(
    private val sagaRepository: SagaRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val distributedLockPort: DistributedLockPort
) {
    fun startPaymentSaga(
        reservationId: Long,
        userId: String,
        seatId: Long,
        concertDateId: Long,
        pointsToUse: Int,
        totalAmount: Int
    ): String {
        val sagaId = UUID.randomUUID().toString()

        return distributedLockPort.executeWithLock(
            lockKey = "lock:payment:reservation:$reservationId",
            waitTime = 10L,
            leaseTime = 30L
        ) {
            val context = PaymentSagaContext(
                sagaId = sagaId,
                reservationId = reservationId,
                userId = userId,
                seatId = seatId,
                concertDateId = concertDateId,
                pointsToUse = pointsToUse,
                totalAmount = totalAmount
            )
            sagaRepository.save(context)

            eventPublisher.publishEvent(
                PaymentInitiatedEvent(
                    sagaId = sagaId,
                    userId = userId,
                    reservationId = reservationId,
                    seatId = seatId,
                    pointsToUse = pointsToUse,
                    totalAmount = totalAmount,
                    timestamp = LocalDateTime.now()
                )
            )
            sagaId
        }
    }

    @EventListener
    @Order(1)
    fun on(event: PaymentInitiatedEvent) {
        val context = updateSagaState(event.sagaId, PaymentSagaState.POINT_DEDUCTING)

        eventPublisher.publishEvent(
            PointDeductCommand(
                sagaId = event.sagaId,
                userId = event.userId,
                amount = event.pointsToUse + event.totalAmount
            )
        )
    }

    @EventListener
    @Order(2)
    fun on(event: PointDeductedEvent) {
        val context = updateSagaState(event.sagaId, PaymentSagaState.POINT_DEDUCTED)
        context.pointTransactionId = event.transactionId
        context.completedSteps.add(PaymentSagaState.POINT_DEDUCTED)
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            SeatConfirmCommand(
                sagaId = event.sagaId,
                seatId = context.seatId
            )
        )
    }

    @EventListener
    @Order(3)
    fun on(event: SeatConfirmedEvent) {
        val context = updateSagaState(event.sagaId, PaymentSagaState.SEAT_CONFIRMED)
        context.completedSteps.add(PaymentSagaState.SEAT_CONFIRMED)
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            ReservationConfirmCommand(
                sagaId = event.sagaId,
                reservationId = context.reservationId,
                userId = context.userId,
                seatId = context.seatId,
                concertDateId = context.concertDateId,
                paymentAmount = context.totalAmount
            )
        )
    }

    @EventListener
    @Order(4)
    fun on(event: ReservationConfirmedEvent) {
        val context = updateSagaState(event.sagaId, PaymentSagaState.RESERVATION_CONFIRMED)
        context.actualReservationId = event.actualReservationId
        context.completedSteps.add(PaymentSagaState.RESERVATION_CONFIRMED)
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            CreatePaymentCommand(
                sagaId = event.sagaId,
                reservationId = context.reservationId,
                userId = context.userId,
                totalAmount = context.totalAmount,
                pointsUsed = context.pointsToUse
            )
        )
    }

    @EventListener
    @Order(5)
    fun on(event: PaymentCreatedEvent) {
        val context = updateSagaState(event.sagaId, PaymentSagaState.COMPLETED)
        context.paymentId = event.paymentId
        context.completedAt = LocalDateTime.now()
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            PaymentSagaCompletedEvent(
                sagaId = event.sagaId,
                userId = context.userId,
                paymentId = event.paymentId
            )
        )
        sagaRepository.delete(event.sagaId)
    }

    @EventListener
    @Order(10)
    fun on(event: SeatReleasedEvent) {
        val context = sagaRepository.findById(event.sagaId)
            ?: throw IllegalStateException("Saga not found: ${event.sagaId}")

        context.compensatedSteps.add(PaymentSagaState.SEAT_RELEASED)
        sagaRepository.save(context)
        eventPublisher.publishEvent(
            PointRefundCommand(
                sagaId = event.sagaId,
                userId = context.userId,
                amount = context.pointsToUse + context.totalAmount
            )
        )
    }

    @EventListener
    @Order(11)
    fun on(event: PointRefundedEvent) {
        val context = sagaRepository.findById(event.sagaId)
            ?: throw IllegalStateException("Saga not found: ${event.sagaId}")

        context.compensatedSteps.add(PaymentSagaState.POINT_REFUNDED)
        context.state = PaymentSagaState.FAILED
        context.completedAt = LocalDateTime.now()
        sagaRepository.save(context)

        eventPublisher.publishEvent(
            PaymentSagaFailedEvent(
                sagaId = event.sagaId,
                userId = context.userId,
                failureReason = context.failureReason ?: "Unknown"
            )
        )
        sagaRepository.delete(event.sagaId)
    }

    private fun updateSagaState(sagaId: String, newState: PaymentSagaState): PaymentSagaContext {
        val context = sagaRepository.findById(sagaId)
            ?: throw IllegalStateException("Saga not found: $sagaId")
        context.state = newState
        sagaRepository.save(context)
        return context
    }
}