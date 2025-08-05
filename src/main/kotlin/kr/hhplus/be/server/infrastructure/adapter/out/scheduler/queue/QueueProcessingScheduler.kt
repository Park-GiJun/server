package kr.hhplus.be.server.infrastructure.adapter.out.scheduler.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessage
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessagePort
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketSessionPort
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@EnableScheduling
class UnifiedQueueScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository,
    private val queueTokenRepository: QueueTokenRepository,
    private val webSocketSessionPort: QueueWebSocketSessionPort,
    private val webSocketMessagePort: QueueWebSocketMessagePort
) {
    private val log = LoggerFactory.getLogger(UnifiedQueueScheduler::class.java)

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    fun processQueueManagement() {
        val startTime = System.currentTimeMillis()
        log.debug("대기열 관리 스케줄러 시작 - ${LocalDateTime.now()}")

        try {
            val concerts = concertRepository.findConcertList()
            var totalActivated = 0
            var totalPositionUpdated = 0

            concerts.forEach { concert ->
                try {
                    val activationResult = processTokenActivation(concert.concertId)
                    totalActivated += activationResult

                    val positionResult = updateQueuePositions(concert.concertId)
                    totalPositionUpdated += positionResult

                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} 대기열 관리 중 오류", e)
                }
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            if (totalActivated > 0 || totalPositionUpdated > 0) {
                log.info("대기열 관리 완료 (${duration}ms): 활성화 ${totalActivated}개, 포지션 업데이트 ${totalPositionUpdated}개")
            }

        } catch (e: Exception) {
            log.error("대기열 관리 스케줄러 처리 중 오류", e)
        }
    }

    private fun processTokenActivation(concertId: Long): Int {
        return try {
            val currentActiveSessions = webSocketSessionPort.getActiveSessionCount(concertId)
            log.debug("콘서트 ${concertId}: 현재 활성 세션 $currentActiveSessions 개")

            val result = processQueueActivationUseCase.processActivation(
                ProcessQueueActivationCommand(concertId)
            )

            if (result.activatedTokens.isNotEmpty()) {
                log.info("콘서트 ${concertId}: ${result.activatedTokens.size}개 토큰 활성화")

                result.activatedTokens.forEach { tokenId ->
                    log.debug("활성화: $tokenId")
                }
            }

            result.activatedTokens.size
        } catch (e: Exception) {
            log.error("콘서트 ${concertId} 토큰 활성화 실패", e)
            0
        }
    }

    private fun updateQueuePositions(concertId: Long): Int {
        return try {
            val waitingTokens = queueTokenRepository.findWaitingTokensByConcert(concertId)

            if (waitingTokens.isEmpty()) {
                log.debug("콘서트 ${concertId}: 대기 중인 토큰 없음")
                return 0
            }

            var positionUpdatedCount = 0

            waitingTokens.forEachIndexed { index, token ->
                try {
                    val currentPosition = index + 1 // 1부터 시작하는 순서

                    val session = webSocketSessionPort.getSession(token.queueTokenId)
                    if (session != null) {
                        val message = QueueWebSocketMessage(
                            tokenId = token.queueTokenId,
                            userId = token.userId,
                            concertId = concertId,
                            status = QueueTokenStatus.WAITING,
                            position = currentPosition,
                            message = "대기 중입니다. 순서: ${currentPosition}"
                        )

                        val success = webSocketMessagePort.sendMessage(token.queueTokenId, message)
                        if (success) {
                            positionUpdatedCount++
                            log.debug("포지션 업데이트: ${token.queueTokenId} -> $currentPosition")
                        } else {
                            log.debug("포지션 업데이트 실패: ${token.queueTokenId}")
                        }
                    }

                } catch (e: Exception) {
                    log.error("토큰 ${token.queueTokenId} 포지션 업데이트 실패", e)
                }
            }

            if (positionUpdatedCount > 0) {
                log.debug("콘서트 ${concertId}: ${positionUpdatedCount}개 포지션 업데이트")
            }

            positionUpdatedCount
        } catch (e: Exception) {
            log.error("콘서트 ${concertId} 포지션 업데이트 실패", e)
            0
        }
    }
}