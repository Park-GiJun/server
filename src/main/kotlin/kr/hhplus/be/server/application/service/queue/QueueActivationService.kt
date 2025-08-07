package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationResult
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.domain.queue.service.QueueActivationDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueActivationService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueEventPort: QueueEventPort
) : ProcessQueueActivationUseCase {

    private val log = LoggerFactory.getLogger(QueueActivationService::class.java)
    private val queueDomainService = QueueActivationDomainService()

    override fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult {
        log.debug("대기열 활성화 처리: concertId=${command.concertId}")

        val activeTokenCount = queueTokenRepository.countActiveTokensByConcert(command.concertId)
        log.info("콘서트 ${command.concertId} - 현재 활성 토큰: $activeTokenCount")

        val tokensToActivate = queueDomainService.calculateTokensToActivate(activeTokenCount)

        if (tokensToActivate <= 0) {
            log.debug("활성화할 토큰 없음: concertId=${command.concertId}")
            return ProcessQueueActivationResult(
                concertId = command.concertId,
                activeTokenCount = activeTokenCount,
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

        log.info("토큰 활성화 완료: concertId=${command.concertId}, 활성화된 수=${activatedTokens.size}")

        return ProcessQueueActivationResult(
            concertId = command.concertId,
            activeTokenCount = activeTokenCount,
            tokensToActivate = tokensToActivate,
            activatedTokens = activatedTokens.map { it.queueTokenId },
            message = "성공적으로 ${activatedTokens.size}개 토큰 활성화"
        )
    }
}