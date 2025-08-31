package kr.hhplus.be.server.application.dto.payment.command.step

import kr.hhplus.be.server.application.dto.concert.command.SeatConfirmCommand
import kr.hhplus.be.server.application.dto.concert.command.SeatReleaseCommand
import kr.hhplus.be.server.application.dto.event.concert.SeatConfirmedEvent
import kr.hhplus.be.server.application.dto.event.concert.SeatReleasedEvent
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.saga.payment.PaymentSagaState
import kr.hhplus.be.server.domain.concert.exception.ConcertSeatNotFoundException
import kr.hhplus.be.server.domain.concert.exception.SeatAlreadyBookedException
import kr.hhplus.be.server.domain.saga.exception.CompensationSagaException
import kr.hhplus.be.server.domain.saga.exception.SeatConfirmationSagaException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class SeatStepHandler(
    private val concertSeatRepository: ConcertSeatRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(SeatStepHandler::class.java)

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: SeatConfirmCommand) {
        try {
            log.info("Processing seat confirmation for saga: ${command.sagaId}")

            val seat = concertSeatRepository.findByConcertSeatId(command.seatId)
                ?: throw ConcertSeatNotFoundException(command.seatId)

            if (seat.isSold()) {
                throw SeatAlreadyBookedException(seat.seatNumber)
            }

            seat.sell()
            concertSeatRepository.save(seat)

            eventPublisher.publishEvent(
                SeatConfirmedEvent(
                    sagaId = command.sagaId,
                    userId = "",
                    seatId = command.seatId
                )
            )

        } catch (e: ConcertSeatNotFoundException) {
            throw SeatConfirmationSagaException(
                sagaId = command.sagaId,
                seatId = command.seatId,
                reason = "Seat not found",
                cause = e
            )
        } catch (e: SeatAlreadyBookedException) {
            throw SeatConfirmationSagaException(
                sagaId = command.sagaId,
                seatId = command.seatId,
                reason = "Seat already sold",
                cause = e
            )
        } catch (e: Exception) {
            throw SeatConfirmationSagaException(
                sagaId = command.sagaId,
                seatId = command.seatId,
                reason = "Unexpected error: ${e.message}",
                cause = e
            )
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: SeatReleaseCommand) {
        try {
            log.info("Processing seat release for saga: ${command.sagaId}")

            val seat = concertSeatRepository.findByConcertSeatId(command.seatId)
            if (seat == null) {
                log.warn("Seat not found for release, skipping: ${command.seatId}")
                eventPublisher.publishEvent(
                    SeatReleasedEvent(
                        sagaId = command.sagaId,
                        userId = "",
                        seatId = command.seatId
                    )
                )
                return
            }

            seat.reserve()
            concertSeatRepository.save(seat)

            eventPublisher.publishEvent(
                SeatReleasedEvent(
                    sagaId = command.sagaId,
                    userId = "",
                    seatId = command.seatId
                )
            )

        } catch (e: Exception) {
            log.error("Seat release failed: ${e.message}")
            throw CompensationSagaException(
                sagaId = command.sagaId,
                step = PaymentSagaState.SEAT_RELEASING,
                reason = "Seat release failed: ${e.message}",
                cause = e
            )
        }
    }
}