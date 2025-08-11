package kr.hhplus.be.server.infrastructure.adapter.out.scheduler.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessagePort
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class RedisQueueScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository,
    private val queueManagementService: RedisQueueManagementService,
    private val webSocketMessagePort: QueueWebSocketMessagePort
) {
    private val log = LoggerFactory.getLogger(RedisQueueScheduler::class.java)

    @Scheduled(fixedDelay = 3000)
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

    private fun processRedisQueue(concertId: Long): Int {
        val stats = queueManagementService.getQueueStats(concertId)

        if (stats.waitingCount == 0L) return 0

        val result = processQueueActivationUseCase.processActivation(
            ProcessQueueActivationCommand(concertId)
        )

        return result.activatedTokens.size
    }

    @Scheduled(fixedDelay = 60000) // 1분마다 정리 작업
    fun cleanupExpiredTokens() {
        log.debug("Redis 만료 토큰 정리 시작")
    }
}