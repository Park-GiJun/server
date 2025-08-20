package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.ExpireQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult
import kr.hhplus.be.server.application.port.`in`.queue.CompleteQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.redis.queue.RedisQueueManagementService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class RedisQueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val redisQueueDomainService: RedisQueueDomainService,
    private val queueManagementService: RedisQueueManagementService
) : GenerateQueueTokenUseCase, ValidateQueueTokenUseCase, ExpireQueueTokenUseCase, CompleteQueueTokenUseCase {

    private val log = LoggerFactory.getLogger(RedisQueueCommandService::class.java)

    override fun generateToken(command: GenerateQueueTokenCommand): GenerateQueueTokenResult {
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findByUserIdAndConcertId(
            command.userId, command.concertId
        )

        if (existingToken != null && !existingToken.isExpired()) {
            val position = calculatePosition(existingToken)
            return GenerateQueueTokenResult(
                tokenId = existingToken.queueTokenId,
                position = position,
                estimatedWaitTime = calculateEstimatedWaitTime(position),
                status = existingToken.tokenStatus
            )
        }
        val newToken = redisQueueDomainService.createNewQueueToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        val rank = if (savedToken.isWaiting()) {
            queueManagementService.addToWaitingQueue(savedToken)
        } else 0L

        val position = redisQueueDomainService.calculateWaitingPosition(rank)
        return GenerateQueueTokenResult(
            tokenId = savedToken.queueTokenId,
            position = position,
            estimatedWaitTime = calculateEstimatedWaitTime(position),
            status = savedToken.tokenStatus
        )
    }

    override fun validateActiveToken(command: ValidateQueueTokenCommand): ValidateQueueTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isExpired()) {
            val expiredToken = token.copy(tokenStatus = QueueTokenStatus.EXPIRED)
            queueTokenRepository.save(expiredToken)
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        return ValidateQueueTokenResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            isValid = true,
            createdAt = token.createdAt,
            enteredAt= token.enteredAt,
        )
    }

    override fun validateActiveTokenForConcert(command: ValidateQueueTokenCommand): ValidateQueueTokenResult {
        val result = validateActiveToken(command)

        command.concertId?.let { expectedConcertId ->
            if (result.concertId != expectedConcertId) {
                throw InvalidTokenException(
                    "Token concert ID mismatch. Expected: $expectedConcertId, Actual: ${result.concertId}"
                )
            }
        }

        return result
    }

    override fun expireToken(command: ExpireQueueTokenCommand): ExpireQueueTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isActive()) {
            queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        }

        val expiredToken = token.copy(
            tokenStatus = QueueTokenStatus.EXPIRED,
            expiresAt = java.time.LocalDateTime.now()
        )
        queueTokenRepository.save(expiredToken)
        return ExpireQueueTokenResult(success = true)
    }

    override fun completeToken(command: CompleteQueueTokenCommand): CompleteQueueTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isActive()) {
            queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        }

        val completedToken = token.copy(
            tokenStatus = QueueTokenStatus.COMPLETED,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5) // 5분 후 정리
        )
        queueTokenRepository.save(completedToken)
        return CompleteQueueTokenResult(success = true)
    }

    private fun calculatePosition(token: QueueToken): Int {
        return if (token.isWaiting()) {
            val rank = queueManagementService.getWaitingPosition(token.concertId, token.userId)
            redisQueueDomainService.calculateWaitingPosition(rank)
        } else 0
    }

    private fun calculateEstimatedWaitTime(position: Int): Int {
        return if (position > 0) {
            // 10명당 1분 대기로 가정
            (position / 10).coerceAtLeast(1)
        } else 0
    }
}