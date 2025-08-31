package kr.hhplus.be.server.infrastructure.adapter.out.scheduler.queue

import kr.hhplus.be.server.application.dto.queue.command.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.event.queue.QueuePositionUpdate
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Redis 대기열 스케줄러
 */
@Component
@EnableScheduling
class RedisQueueScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository,
    private val queueManagementService: RedisQueueManagementService,
    private val queueEventPort: QueueEventPort,
    private val queueTokenRepository: QueueTokenRepository
) {
    private val log = LoggerFactory.getLogger(RedisQueueScheduler::class.java)

    /**
     * 대기열 활성화 처리 (3초마다)
     */
    @Scheduled(fixedDelay = 3000)
    fun processQueueActivation() {
        try {
            val concerts = concertRepository.findConcertList()
            var totalActivated = 0

            concerts.forEach { concert ->
                try {
                    val result = processQueueActivationUseCase.processActivation(
                        ProcessQueueActivationCommand(concert.concertId)
                    )

                    if (result.activatedTokens.isNotEmpty()) {
                        totalActivated += result.activatedTokens.size
                        log.info("토큰 활성화: concertId=${concert.concertId}, 활성화=${result.activatedTokens.size}개")
                    }

                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} 활성화 처리 중 오류", e)
                }
            }

            if (totalActivated > 0) {
                log.info("전체 활성화 완료: $totalActivated 개")
            }

        } catch (e: Exception) {
            log.error("대기열 스케줄러 오류", e)
        }
    }

    /**
     * 대기열 위치 업데이트
     */
    @Scheduled(fixedDelay = 5000)
    fun processPositionUpdates() {
        try {
            val concerts = concertRepository.findConcertList()
            var totalUpdated = 0

            concerts.forEach { concert ->
                try {
                    val updated = processPositionUpdatesForConcert(concert.concertId)
                    totalUpdated += updated
                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} 위치 업데이트 중 오류", e)
                }
            }

            if (totalUpdated > 0) {
                log.info("위치 업데이트 완료: $totalUpdated 명")
            }

        } catch (e: Exception) {
            log.error("위치 업데이트 스케줄러 오류", e)
        }
    }

    /**
     * 콘서트별 위치 업데이트 처리
     */
    private fun processPositionUpdatesForConcert(concertId: Long): Int {
        val stats = queueManagementService.getQueueStats(concertId)

        if (stats.waitingCount == 0L) {
            return 0
        }

        val waitingTokens = queueTokenRepository.findWaitingTokensByConcert(concertId, 100)

        if (waitingTokens.isEmpty()) {
            return 0
        }

        val positionUpdates = mutableListOf<QueuePositionUpdate>()

        waitingTokens.forEach { token ->
            try {
                val currentPosition = queueManagementService.getWaitingPosition(concertId, token.userId)

                if (currentPosition >= 0) {
                    val displayPosition = currentPosition + 1
                    val estimatedWaitTime = calculateEstimatedWaitTime(displayPosition)

                    positionUpdates.add(
                        QueuePositionUpdate(
                            tokenId = token.queueTokenId,
                            userId = token.userId,
                            newPosition = displayPosition,
                            estimatedWaitTime = estimatedWaitTime
                        )
                    )
                }
            } catch (e: Exception) {
                log.error("토큰 위치 조회 중 오류: tokenId=${token.queueTokenId}", e)
            }
        }

        if (positionUpdates.isNotEmpty()) {
            try {
                positionUpdates.forEach { update ->
                    queueEventPort.publishQueueEntered(
                        tokenId = update.tokenId,
                        userId = update.userId,
                        concertId = concertId,
                        position = update.newPosition,
                        estimatedWaitTime = update.estimatedWaitTime
                    )
                }

                log.debug("위치 업데이트 발행: concertId=$concertId, 업데이트=${positionUpdates.size}개")
            } catch (e: Exception) {
                log.error("위치 업데이트 이벤트 발행 중 오류: concertId=$concertId", e)
            }
        }

        return positionUpdates.size
    }

    /**
     * 만료된 토큰 정리 (1분마다)
     */
    @Scheduled(fixedDelay = 60000)
    fun cleanupExpiredTokens() {
        try {
            val concerts = concertRepository.findConcertList()
            var totalCleaned = 0

            concerts.forEach { concert ->
                try {
                    val expired = queueManagementService.cleanupExpiredActiveTokens(concert.concertId)
                    totalCleaned += expired.size

                    if (expired.isNotEmpty()) {
                        log.info("만료 토큰 정리: concertId=${concert.concertId}, 정리=${expired.size}개")
                    }
                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} 만료 토큰 정리 중 오류", e)
                }
            }

            if (totalCleaned > 0) {
                log.info("만료 토큰 정리 완료: $totalCleaned 개")
            }

        } catch (e: Exception) {
            log.error("만료 토큰 정리 중 오류", e)
        }
    }
    /**
     * 예상 대기 시간 계산
     */
    private fun calculateEstimatedWaitTime(position: Long): Int {
        val processingRate = 10
        val estimatedMinutes = (position / processingRate).toInt()

        return when {
            estimatedMinutes < 1 -> 1
            estimatedMinutes > 60 -> 60
            else -> estimatedMinutes
        }
    }
}