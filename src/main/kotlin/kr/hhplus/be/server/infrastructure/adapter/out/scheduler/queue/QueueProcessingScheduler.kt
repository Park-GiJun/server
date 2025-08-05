package kr.hhplus.be.server.infrastructure.adapter.out.scheduler

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketSessionPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class QueueProcessingScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository,
    private val webSocketSessionPort: QueueWebSocketSessionPort
) {
    private val log = LoggerFactory.getLogger(QueueProcessingScheduler::class.java)

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    fun processQueueActivation() {
        log.debug("대기열 활성화 스케줄러 시작")

        try {
            val concerts = concertRepository.findConcertList()

            concerts.forEach { concert ->
                try {
                    val currentActiveSessions = webSocketSessionPort.getActiveSessionCount(concert.concertId)

                    log.debug("콘서트 ${concert.concertId}: 현재 활성 세션 $currentActiveSessions 개")

                    val result = processQueueActivationUseCase.processActivation(
                        ProcessQueueActivationCommand(concert.concertId)
                    )

                    if (result.activatedTokens.isNotEmpty()) {
                        log.info("콘서트 ${concert.concertId}: ${result.message}")
                    }

                } catch (e: Exception) {
                    log.error("콘서트 ${concert.concertId} 대기열 처리 중 오류", e)
                }
            }

        } catch (e: Exception) {
            log.error("대기열 스케줄러 처리 중 오류", e)
        }
    }
}