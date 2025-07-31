package kr.hhplus.be.server.infrastructure.adapter.out.scheduler

import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.UpdateQueuePositionsCommand
import kr.hhplus.be.server.application.port.`in`.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.UpdateQueuePositionsUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueActivationEvent
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
        private const val MAX_ACTIVE_USERS_PER_CONCERT = 10
    }

    @Scheduled(cron = "30 * * * * *")
    fun processQueueActivation() {
        val concerts = concertRepository.findConcertList()
        concerts.forEach { concert ->
            processQueueForConcert(concert.concertId)
        }
    }

    @Scheduled(fixedDelay = 15000)
    fun updateQueuePositions() {
        log.debug("Updating queue positions for all concerts")

        val concerts = concertRepository.findConcertList()
        concerts.forEach { concert ->
            updateQueuePositionsForConcert(concert.concertId)
        }
    }

    private fun processQueueForConcert(concertId: Long) {
        val currentActiveCount = queueWebSocketService.getActiveSessionCount(concertId)
        val slotsAvailable = MAX_ACTIVE_USERS_PER_CONCERT - currentActiveCount
        log.info("Processing queue for concert $concertId, available slots: $slotsAvailable")
        if (slotsAvailable <= 0) return
        val activationResult = activateTokensUseCase.activateTokens(
            ActivateTokensCommand(concertId, slotsAvailable)
        )

        if (activationResult.activatedCount > 0) {
            log.info("Activated ${activationResult.activatedCount} tokens for concert $concertId")
        }
    }

    private fun updateQueuePositionsForConcert(concertId: Long) {
        val updateResult = updateQueuePositionsUseCase.updateQueuePositions(
            UpdateQueuePositionsCommand(concertId)
        )

        if (updateResult.updatedCount == 0) {
            log.debug("No position changes found for concert $concertId")
            return
        }

        updateResult.positionChanges.forEach { change ->
            log.info("Token ${change.token.queueTokenId} entered at ${change.token.enteredAt}")
            log.info("Updated Position from ${change.oldPosition} to ${change.newPosition} for Concert $concertId")

            queueWebSocketService.sendQueueUpdate(
                tokenId = change.token.queueTokenId,
                position = change.newPosition,
                status = change.token.tokenStatus,
                message = "대기열 위치가 업데이트되었습니다."
            )
        }

        log.info("Updated ${updateResult.updatedCount} queue positions for concert $concertId")
    }

    @Scheduled(fixedDelay = 60000)
    fun cleanupExpiredSessions() {
        log.debug("Cleaning up expired WebSocket sessions")
    }
}