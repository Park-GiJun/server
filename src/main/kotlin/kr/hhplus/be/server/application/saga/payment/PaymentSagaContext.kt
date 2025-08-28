package kr.hhplus.be.server.application.saga.payment

import java.time.LocalDateTime

data class PaymentSagaContext(
    val sagaId: String,
    val reservationId: Long,
    val userId: String,
    val seatId: Long,
    val concertDateId: Long,
    val pointsToUse: Int,
    val totalAmount: Int,
    var state: PaymentSagaState = PaymentSagaState.STARTED,

    var pointTransactionId: String? = null,
    var actualReservationId: Long? = null,
    var paymentId: Long? = null,

    val completedSteps: MutableSet<PaymentSagaState> = mutableSetOf(),
    val compensatedSteps: MutableSet<PaymentSagaState> = mutableSetOf(),

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var completedAt: LocalDateTime? = null,
    var failureReason: String? = null
)