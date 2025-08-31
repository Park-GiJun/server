package kr.hhplus.be.server.application.handler.command.reservation.step

import kr.hhplus.be.server.application.dto.concert.command.SeatReleaseCommand
import kr.hhplus.be.server.application.dto.event.reservation.ReservationConfirmedEvent
import kr.hhplus.be.server.application.dto.reservation.command.ReservationCancelCommand
import kr.hhplus.be.server.application.dto.reservation.command.ReservationConfirmCommand
import kr.hhplus.be.server.application.port.out.reservation.ReservationRepository
import kr.hhplus.be.server.application.port.out.reservation.TempReservationRepository
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import kr.hhplus.be.server.domain.reservation.exception.ReservationExpiredException
import kr.hhplus.be.server.domain.reservation.exception.TempReservationValidationException
import kr.hhplus.be.server.domain.saga.exception.CompensationSagaException
import kr.hhplus.be.server.domain.saga.exception.ReservationConfirmationSagaException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ReservationStepHandler(
    private val reservationRepository: ReservationRepository,
    private val tempReservationRepository: TempReservationRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: ReservationConfirmCommand) {
        try {
            val tempReservation = tempReservationRepository.findByUserIdAndConcertSeatId(
                command.userId, command.seatId
            ) ?: throw TempReservationValidationException(command.userId, command.seatId)

            if (tempReservation.isExpired()) {
                throw ReservationExpiredException(tempReservation.tempReservationId) as Throwable
            }

            val reservation = Reservation(
                reservationId = 0L,
                userId = command.userId,
                concertDateId = command.concertDateId,
                seatId = command.seatId,
                paymentAmount = command.paymentAmount,
                reservationStatus = ReservationStatus.CONFIRMED,
                reservationAt = LocalDateTime.now()
            )

            val savedReservation = reservationRepository.save(reservation)

            tempReservationRepository.delete(tempReservation)

            eventPublisher.publishEvent(
                ReservationConfirmedEvent(
                    sagaId = command.sagaId,
                    userId = command.userId,
                    reservationId = command.reservationId,
                    actualReservationId = savedReservation.reservationId
                )
            )

        } catch (e: TempReservationValidationException) {
            throw ReservationConfirmationSagaException(
                sagaId = command.sagaId,
                reservationId = command.reservationId,
                reason = "Temp reservation not found",
                cause = e
            )
        } catch (e: ReservationExpiredException) {
            throw ReservationConfirmationSagaException(
                sagaId = command.sagaId,
                reservationId = command.reservationId,
                reason = "Temp reservation expired",
                cause = e
            )
        } catch (e: Exception) {
            throw ReservationConfirmationSagaException(
                sagaId = command.sagaId,
                reservationId = command.reservationId,
                reason = "Unexpected error: ${e.message}",
                cause = e
            )
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: ReservationCancelCommand) {
        try {
            val reservation = reservationRepository.findByReservationId(command.reservationId)
            if (reservation == null) {
                eventPublisher.publishEvent(
                    SeatReleaseCommand(
                        sagaId = command.sagaId,
                        seatId = 0L
                    )
                )
                return
            }

            reservation.cancel()
            reservationRepository.save(reservation)

            eventPublisher.publishEvent(
                SeatReleaseCommand(
                    sagaId = command.sagaId,
                    seatId = reservation.seatId
                )
            )

        } catch (e: Exception) {
            throw CompensationSagaException(
                sagaId = command.sagaId,
                step = PaymentSagaState.RESERVATION_CANCELLING,
                reason = "Reservation cancel failed: ${e.message}",
                cause = e
            )
        }
    }
}