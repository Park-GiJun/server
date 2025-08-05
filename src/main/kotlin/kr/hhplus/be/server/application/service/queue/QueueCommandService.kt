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
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.service.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueueActivationEvent
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto.QueuePositionUpdateEvent
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
        log.info("대기열 토큰 생성: userId=${command.userId}, concertId=${command.concertId}")

        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId, command.concertId
        )

        if (existingToken != null) {
            log.info("기존 토큰 반환: tokenId=${existingToken.queueTokenId}")
            return existingToken.queueTokenId
        }

        val newToken = queueDomainService.createNewToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        log.info("새 토큰 생성 완료: tokenId=${savedToken.queueTokenId}")
        return savedToken.queueTokenId
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val validatedOrExpiredToken = queueDomainService.validateTokenAndExpireIfNeeded(token)

        if (validatedOrExpiredToken.isExpired()) {
            queueTokenRepository.save(validatedOrExpiredToken)
            queueWebSocketEventPublisher.publishExpiration(command.tokenId)
            log.warn("토큰 만료: tokenId=${command.tokenId}")
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
        log.info("토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val expiredToken = queueDomainService.expireToken(token)
        queueTokenRepository.save(expiredToken)

        queueWebSocketEventPublisher.publishExpiration(command.tokenId)

        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        log.info("토큰 완료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val completedToken = queueDomainService.completeToken(token)
        queueTokenRepository.save(completedToken)
        return true
    }

    override fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult {
        log.info("대기열 토큰 활성화: concertId=${command.concertId}, 요청수=${command.count}")

        val currentActiveCount = queueTokenRepository.countActiveTokensByConcert(command.concertId)
        val maxActiveUsers = 3
        val actualSlotsAvailable = maxActiveUsers - currentActiveCount

        if (actualSlotsAvailable <= 0) {
            log.info("활성화 가능한 슬롯 없음: 현재활성=${currentActiveCount}")
            return ActivateTokensResult(0, emptyList())
        }

        val tokensToActivate = minOf(command.count, actualSlotsAvailable)

        val activatedTokens = queueTokenRepository.activateWaitingTokens(command.concertId, tokensToActivate)

        if (activatedTokens.isEmpty()) {
            log.info("활성화할 대기 토큰 없음")
            return ActivateTokensResult(0, emptyList())
        }

        activatedTokens.forEach { token ->
            try {
                queueWebSocketEventPublisher.publishActivation(
                    QueueActivationEvent(
                        tokenId = token.queueTokenId,
                        userId = token.userId,
                        concertId = token.concertId
                    )
                )
            } catch (e: Exception) {
                log.error("토큰 활성화 이벤트 발송 실패: tokenId=${token.queueTokenId}", e)
            }
        }

        log.info("토큰 활성화 완료: ${activatedTokens.size}개")
        return ActivateTokensResult(
            activatedCount = activatedTokens.size,
            tokenIds = activatedTokens.map { it.queueTokenId }
        )
    }

    override fun updateQueuePositions(command: UpdateQueuePositionsCommand): UpdateQueuePositionsResult {
        val waitingTokens = queueTokenRepository.findWaitingTokensByConcertIdOrderByEnteredAt(command.concertId)

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

        positionChanges.forEach { change ->
            try {
                queueWebSocketEventPublisher.publishPositionUpdate(
                    QueuePositionUpdateEvent(
                        tokenId = change.token.queueTokenId,
                        userId = change.token.userId,
                        concertId = command.concertId,
                        newPosition = change.newPosition,
                        status = change.token.tokenStatus
                    )
                )
            } catch (e: Exception) {
                log.error("대기순서 업데이트 이벤트 발송 실패: tokenId=${change.token.queueTokenId}", e)
            }
        }

        if (positionChanges.isNotEmpty()) {
            log.info("대기순서 업데이트 완료: concertId=${command.concertId}, 변경건수=${positionChanges.size}")
        }

        return UpdateQueuePositionsResult(
            concertId = command.concertId,
            updatedCount = positionChanges.size,
            positionChanges = positionChanges
        )
    }
}