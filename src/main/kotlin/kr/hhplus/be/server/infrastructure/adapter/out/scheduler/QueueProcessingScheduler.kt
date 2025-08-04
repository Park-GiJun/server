package kr.hhplus.be.server.infrastructure.adapter.out.scheduler

import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.UpdateQueuePositionsCommand
import kr.hhplus.be.server.application.port.`in`.queue.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.queue.UpdateQueuePositionsUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.service.QueueWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
@EnableAsync
class QueueProcessingScheduler(
    private val activateTokensUseCase: ActivateTokensUseCase,
    private val updateQueuePositionsUseCase: UpdateQueuePositionsUseCase,
    private val concertRepository: ConcertRepository,
    private val queueWebSocketService: QueueWebSocketService
) {
    private val log = LoggerFactory.getLogger(QueueProcessingScheduler::class.java)

    companion object {
        private const val MAX_ACTIVE_USERS_PER_CONCERT = 3
    }

    @Scheduled(fixedDelay = 10000)
    fun processQueueActivation() {
        log.debug("Starting queue activation process for all concerts")

        val concerts = concertRepository.findConcertList()
        concerts.forEach { concert ->
            processQueueForConcert(concert.concertId)
        }

        log.debug("Completed queue activation process for ${concerts.size} concerts")
    }

    @Scheduled(fixedDelay = 15000)
    fun updateQueuePositions() {
        log.debug("Updating queue positions for all concerts")

        val concerts = concertRepository.findConcertList()
        concerts.forEach { concert ->
            updateQueuePositionsForConcert(concert.concertId)
        }

        log.debug("Completed queue position updates for ${concerts.size} concerts")
    }

    private fun processQueueForConcert(concertId: Long) {
        try {
            val currentActiveCount = queueWebSocketService.getActiveSessionCount(concertId)
            val slotsAvailable = MAX_ACTIVE_USERS_PER_CONCERT - currentActiveCount

            log.info("Processing queue for concert $concertId - Current active: $currentActiveCount, Available slots: $slotsAvailable")

            if (slotsAvailable <= 0) {
                log.debug("No available slots for concert $concertId, skipping activation")
                return
            }

            val activationResult = activateTokensUseCase.activateTokens(
                ActivateTokensCommand(concertId, slotsAvailable)
            )

            if (activationResult.activatedCount > 0) {
                log.info("Activated ${activationResult.activatedCount} tokens for concert $concertId")
            } else {
                log.debug("No waiting tokens found for concert $concertId")
            }

        } catch (e: Exception) {
            log.error("Error processing queue for concert $concertId", e)
        }
    }

    private fun updateQueuePositionsForConcert(concertId: Long) {
        try {
            val updateResult = updateQueuePositionsUseCase.updateQueuePositions(
                UpdateQueuePositionsCommand(concertId)
            )

            if (updateResult.updatedCount == 0) {
                log.debug("No position changes found for concert $concertId")
                return
            }

            updateResult.positionChanges.forEach { change ->
                log.debug("Token ${change.token.queueTokenId} entered at ${change.token.enteredAt}")
                log.info("📍 Updated Position from ${change.oldPosition} to ${change.newPosition} for Concert $concertId")

                queueWebSocketService.sendQueueUpdate(
                    tokenId = change.token.queueTokenId,
                    position = change.newPosition,
                    status = change.token.tokenStatus,
                    message = "대기열 위치가 업데이트되었습니다."
                )
            }

            log.info("Updated ${updateResult.updatedCount} queue positions for concert $concertId")

        } catch (e: Exception) {
            log.error("Error updating queue positions for concert $concertId", e)
        }
    }
}