package kr.hhplus.be.server.infrastructure.adapter.out.scheduler.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Redis 대기열 스케줄러
 * - SSE 이벤트 기반으로 변경됨
 * - WebSocket 의존성 제거
 */
@Component
@EnableScheduling
class RedisQueueScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository,
    private val queueManagementService: RedisQueueManagementService
) {
    private val log = LoggerFactory.getLogger(RedisQueueScheduler::class.java)

    @Scheduled(fixedDelay = 3000) // 3초마다 실행
    fun processRedisQueueManagement() {
        val startTime = System.currentTimeMillis()

        try {
            val concerts = concertRepository.findConcertList()
            var totalProcessed = 0

            concerts.forEach { concert ->
                try {
                    val processed = processRedisQueue(concert.concertId)
                    totalProcessed += processed
                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} Redis 큐 처리 중 오류", e)
                }
            }

            val duration = System.currentTimeMillis() - startTime
            if (totalProcessed > 0) {
                log.info("Redis 큐 처리 완료 (${duration}ms): $totalProcessed 개 처리")
            }

        } catch (e: Exception) {
            log.error("Redis 큐 스케줄러 처리 중 오류", e)
        }
    }

    /**
     * 개별 콘서트 대기열 처리
     */
    private fun processRedisQueue(concertId: Long): Int {
        val stats = queueManagementService.getQueueStats(concertId)

        if (stats.waitingCount == 0L) {
            return 0
        }

        log.debug("Redis 큐 처리: concertId=$concertId, 대기중=${stats.waitingCount}, 활성=${stats.activeCount}")

        val result = processQueueActivationUseCase.processActivation(
            ProcessQueueActivationCommand(concertId)
        )

        if (result.activatedTokens.isNotEmpty()) {
            log.info("큐 활성화 완료: concertId=$concertId, 활성화=${result.activatedTokens.size}개")
        }

        return result.activatedTokens.size
    }

    @Scheduled(fixedDelay = 60000) // 1분마다 정리 작업
    fun cleanupExpiredTokens() {
        log.debug("Redis 만료 토큰 정리 작업 시작")

        try {

        } catch (e: Exception) {
            log.error("만료 토큰 정리 중 오류", e)
        }
    }

    @Scheduled(fixedDelay = 30000) // 30초마다 통계 로깅
    fun logQueueStatistics() {
        try {
            val concerts = concertRepository.findConcertList()
            var totalWaiting = 0L
            var totalActive = 0L
            var activeQueues = 0

            concerts.forEach { concert ->
                val stats = queueManagementService.getQueueStats(concert.concertId)
                if (stats.waitingCount > 0 || stats.activeCount > 0) {
                    totalWaiting += stats.waitingCount
                    totalActive += stats.activeCount
                    activeQueues++

                    if (stats.waitingCount > 0) {
                        log.debug("큐 상태: concertId=${concert.concertId}, 대기=${stats.waitingCount}, 활성=${stats.activeCount}")
                    }
                }
            }

            if (activeQueues > 0) {
                log.info("전체 큐 통계: 활성큐=$activeQueues 개, 총대기=$totalWaiting 명, 총활성=$totalActive 명")
            }

        } catch (e: Exception) {
            log.error("큐 통계 로깅 중 오류", e)
        }
    }
}