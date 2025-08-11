package kr.hhplus.be.server.application.service.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationResult
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RedisQueueActivationService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueEventPort: QueueEventPort,
    private val queueManagementService: RedisQueueManagementService,
    private val redisQueueDomainService: RedisQueueDomainService
) : ProcessQueueActivationUseCase {

    private val log = LoggerFactory.getLogger(RedisQueueActivationService::class.java)

    override fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult {
        log.debug("Redis 대기열 활성화 처리: concertId=${command.concertId}")

        val stats = queueManagementService.getQueueStats(command.concertId)
        val tokensToActivate = redisQueueDomainService.calculateTokensToActivate(stats.activeCount.toInt())

        if (tokensToActivate <= 0) {
            log.debug("활성화할 토큰 없음: concertId=${command.concertId}")
            return ProcessQueueActivationResult(
                concertId = command.concertId,
                activeTokenCount = stats.activeCount.toInt(),
                tokensToActivate = 0,
                activatedTokens = emptyList(),
                message = "최대 활성 토큰 수 도달"
            )
        }

        // Redis 원자적 활성화
        val activatedTokens = queueTokenRepository.activateWaitingTokens(command.concertId, tokensToActivate)

        // 이벤트 발행
        activatedTokens.forEach { token ->
            queueEventPort.publishTokenActivated(
                tokenId = token.queueTokenId,
                userId = token.userId,
                concertId = token.concertId
            )
        }

        log.info("Redis 토큰 활성화 완료: concertId=${command.concertId}, 활성화된 수=${activatedTokens.size}")

        return ProcessQueueActivationResult(
            concertId = command.concertId,
            activeTokenCount = stats.activeCount.toInt() + activatedTokens.size,
            tokensToActivate = tokensToActivate,
            activatedTokens = activatedTokens.map { it.queueTokenId },
            message = "성공적으로 ${activatedTokens.size}개 토큰 활성화"
        )
    }
}