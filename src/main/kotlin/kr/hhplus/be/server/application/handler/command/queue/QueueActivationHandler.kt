package kr.hhplus.be.server.application.handler.command.queue

import kr.hhplus.be.server.application.dto.queue.command.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.result.ProcessQueueActivationResult
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.RedisQueueManagementService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QueueActivationHandler(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueEventPort: QueueEventPort,
    private val queueManagementService: RedisQueueManagementService
) : ProcessQueueActivationUseCase {

    private val redisQueueDomainService = RedisQueueDomainService()


    override fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult {
        val stats = queueManagementService.getQueueStats(command.concertId)
        val tokensToActivate = redisQueueDomainService.calculateTokensToActivate(stats.activeCount.toInt())

        if (tokensToActivate <= 0) {
            return ProcessQueueActivationResult(
                concertId = command.concertId,
                activeTokenCount = stats.activeCount.toInt(),
                tokensToActivate = 0,
                activatedTokens = emptyList(),
                message = "최대 활성 토큰 수 도달"
            )
        }

        val activatedTokens = queueTokenRepository.activateWaitingTokens(command.concertId, tokensToActivate)

        activatedTokens.forEach { token ->
            queueEventPort.publishTokenActivated(
                tokenId = token.queueTokenId,
                userId = token.userId,
                concertId = token.concertId
            )
        }
        return ProcessQueueActivationResult(
            concertId = command.concertId,
            activeTokenCount = stats.activeCount.toInt() + activatedTokens.size,
            tokensToActivate = tokensToActivate,
            activatedTokens = activatedTokens.map { it.queueTokenId },
            message = "성공적으로 ${activatedTokens.size}개 토큰 활성화"
        )
    }
}