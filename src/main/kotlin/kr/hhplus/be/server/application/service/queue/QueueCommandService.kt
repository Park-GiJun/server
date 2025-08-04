package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.*
import kr.hhplus.be.server.application.mapper.QueueMapper
import kr.hhplus.be.server.application.port.`in`.queue.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.queue.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.UpdateQueuePositionsUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueActivationEvent
import kr.hhplus.be.server.infrastructure.adapter.out.event.QueueWebSocketEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val queueWebSocketEventPublisher: QueueWebSocketEventPublisher
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase,
    CompleteTokenUseCase, ActivateTokensUseCase, UpdateQueuePositionsUseCase {

    private val queueDomainService = QueueDomainService()
    private val log = LoggerFactory.getLogger(QueueCommandService::class.java)

    override fun generateToken(command: GenerateTokenCommand): String {
        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId, command.concertId
        )

        if (existingToken != null) {
            return existingToken.queueTokenId
        }

        val newToken = queueDomainService.createNewToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        return savedToken.queueTokenId
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val validatedOrExpiredToken = queueDomainService.validateTokenAndExpireIfNeeded(token)

        if (validatedOrExpiredToken.isExpired()) {
            queueTokenRepository.save(validatedOrExpiredToken)
            queueWebSocketEventPublisher.publishExpiration(command.tokenId)
            throw InvalidTokenStatusException(
                token.tokenStatus,
                QueueTokenStatus.ACTIVE
            )
        }

        val validatedToken = queueDomainService.validateActiveToken(validatedOrExpiredToken)
        return QueueMapper.toValidateResult(validatedToken, true)
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        val tokenResult = validateActiveToken(command)

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        queueDomainService.validateTokenForConcert(token, command.concertId)

        return tokenResult
    }

    override fun expireToken(command: ExpireTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val expiredToken = queueDomainService.expireToken(token)
        queueTokenRepository.save(expiredToken)

        queueWebSocketEventPublisher.publishExpiration(command.tokenId)

        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val completedToken = queueDomainService.completeToken(token)
        queueTokenRepository.save(completedToken)
        return true
    }

    override fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult {
        log.info("Starting token activation for concert ${command.concertId}, requested count: ${command.count}")

        log.info("Calling queueTokenRepository.activateWaitingTokens...")

        val activatedTokens = queueTokenRepository.activateWaitingTokens(command.concertId, command.count)

        log.info("Repository returned ${activatedTokens.size} activated tokens")

        if (activatedTokens.isEmpty()) {
            log.warn("No tokens were activated by repository for concert ${command.concertId}")
            return ActivateTokensResult(0, emptyList())
        }

        activatedTokens.forEachIndexed { index, token ->
            log.info("Activated token [$index]: ${token.queueTokenId}, user: ${token.userId}, status: ${token.tokenStatus}")
        }

        log.info("Publishing activation events...")

        activatedTokens.forEach { token ->
            try {
                queueWebSocketEventPublisher.publishActivation(
                    QueueActivationEvent(
                        tokenId = token.queueTokenId,
                        userId = token.userId,
                        concertId = token.concertId
                    )
                )
                log.debug("Published activation event for token: ${token.queueTokenId}")
            } catch (e: Exception) {
                log.error("Failed to publish activation event for token: ${token.queueTokenId}", e)
            }
        }

        log.info("Token activation completed. Activated: ${activatedTokens.size}, TokenIds: ${activatedTokens.map { it.queueTokenId }}")

        return ActivateTokensResult(
            activatedCount = activatedTokens.size,
            tokenIds = activatedTokens.map { it.queueTokenId }
        )
    }

    override fun updateQueuePositions(command: UpdateQueuePositionsCommand): UpdateQueuePositionsResult {
        val waitingTokens = queueTokenRepository.findWaitingTokensByConcertIdOrderByEnteredAt(command.concertId)

        waitingTokens.forEach { token ->
            log.info("Update tokens ${token.queueTokenId}")
        }


        if (waitingTokens.isEmpty()) {
            return UpdateQueuePositionsResult(
                concertId = command.concertId,
                updatedCount = 0,
                positionChanges = emptyList()
            )
        }

        val newPositions = queueDomainService.calculatePositionByIndex(waitingTokens)

        val oldPositions = waitingTokens.associate { token ->
            token.queueTokenId to (queueTokenRepository.countWaitingTokensBeforeUser(
                token.userId, command.concertId, token.enteredAt
            ) + 1)
        }

        val changedTokenIds = queueDomainService.findChangedTokenIds(oldPositions, newPositions)

        val positionChanges = changedTokenIds.map { tokenId ->
            val token = waitingTokens.first { it.queueTokenId == tokenId }
            QueuePositionChange(
                token = token,
                oldPosition = oldPositions[tokenId] ?: 0,
                newPosition = newPositions[tokenId] ?: 0
            )
        }

        return UpdateQueuePositionsResult(
            concertId = command.concertId,
            updatedCount = positionChanges.size,
            positionChanges = positionChanges
        )
    }
}