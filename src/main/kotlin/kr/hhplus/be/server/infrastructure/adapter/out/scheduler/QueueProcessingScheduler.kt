package kr.hhplus.be.server.infrastructure.adapter.out.scheduler

import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.UpdateQueuePositionsCommand
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.queue.UpdateQueuePositionsUseCase
// ❌ Repository 제거
// import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
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
    private val queueWebSocketService: QueueWebSocketService,
    private val getConcertListUseCase : GetConcertListUseCase
) {
    private val log = LoggerFactory.getLogger(QueueProcessingScheduler::class.java)

    companion object {
        private const val MAX_ACTIVE_USERS_PER_CONCERT = 3
    }

    @Scheduled(fixedDelay = 10000)
    fun processQueueActivation() {
        val concertList = getConcertListUseCase.getConcertList();

        concertList.forEach { concert ->
            processQueueForConcert(concert.concertId)
        }
    }

    @Scheduled(fixedDelay = 15000)
    fun updateQueuePositions() {
        val concertList = getConcertListUseCase.getConcertList();

        concertList.forEach { concert ->
            updateQueuePositionsForConcert(concert.concertId)
        }
    }

    private fun processQueueForConcert(concertId: Long) {
        try {
            val activationResult = activateTokensUseCase.activateTokens(
                ActivateTokensCommand(concertId, MAX_ACTIVE_USERS_PER_CONCERT)
            )

            if (activationResult.activatedCount > 0) {
                log.info("Activated ${activationResult.activatedCount} tokens for concert $concertId")
            } else {
                log.debug("No waiting tokens found or max capacity reached for concert $concertId")
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
                queueWebSocketService.sendQueueUpdate(
                    tokenId = change.token.queueTokenId,
                    position = change.newPosition,
                    status = change.token.tokenStatus,
                    message = "대기열 위치가 업데이트되었습니다."
                )
            }
        } catch (e: Exception) {
            log.error("Error updating queue positions for concert $concertId", e)
        }
    }
}