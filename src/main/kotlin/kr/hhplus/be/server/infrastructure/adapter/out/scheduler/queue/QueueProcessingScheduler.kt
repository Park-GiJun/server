package kr.hhplus.be.server.infrastructure.adapter.out.scheduler.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class QueueProcessingScheduler(
    private val processQueueActivationUseCase: ProcessQueueActivationUseCase,
    private val concertRepository: ConcertRepository
) {
    private val log = LoggerFactory.getLogger(QueueProcessingScheduler::class.java)

    @Scheduled(fixedDelay = 10000)
    fun processQueueActivation() {
        try {
            val concerts = concertRepository.findConcertList()

            concerts.forEach { concert ->
                try {
                    val result = processQueueActivationUseCase.processActivation(
                        ProcessQueueActivationCommand(concert.concertId)
                    )

                    if (result.activatedTokens.isNotEmpty()) {
                        log.info("Concert ${concert.concertId}: ${result.message}")
                    }

                } catch (e: Exception) {
                    log.error("콘서트 대기열 처리중 에러 발생 ${concert.concertId}", e)
                }
            }

            log.debug("대기열 처리 완료 : ${concerts.size} concerts")

        } catch (e: Exception) {
            log.error("콘서트 대기열 스케쥴러 에러 발생", e)
        }
    }
}
