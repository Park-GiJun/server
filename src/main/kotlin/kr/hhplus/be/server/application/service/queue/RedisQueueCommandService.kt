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
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.redis.RedisQueueManagementService
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
        log.info("Redis 대기열 토큰 생성: userId=${command.userId}, concertId=${command.concertId}")

        // 사용자 존재 확인
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        // 기존 토큰 확인 (활성 또는 대기 중인 토큰)
        val existingToken = queueTokenRepository.findByUserIdAndConcertId(
            command.userId, command.concertId
        )

        if (existingToken != null && !existingToken.isExpired()) {
            log.info("기존 토큰 반환: tokenId=${existingToken.queueTokenId}")
            val position = calculatePosition(existingToken)
            return GenerateQueueTokenResult(
                tokenId = existingToken.queueTokenId,
                position = position,
                estimatedWaitTime = calculateEstimatedWaitTime(position),
                status = existingToken.tokenStatus
            )
        }

        // 새 토큰 생성
        val newToken = redisQueueDomainService.createNewQueueToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        // Redis 대기열에 추가 (대기 상태인 경우)
        val rank = if (savedToken.isWaiting()) {
            queueManagementService.addToWaitingQueue(savedToken)
        } else 0L

        val position = redisQueueDomainService.calculateWaitingPosition(rank)

        log.info("Redis 토큰 생성 완료: tokenId=${savedToken.queueTokenId}, position=$position")

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
            // 만료된 토큰 처리
            val expiredToken = token.copy(tokenStatus = QueueTokenStatus.EXPIRED)
            queueTokenRepository.save(expiredToken)
            log.warn("토큰 만료: tokenId=${command.tokenId}")
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        return ValidateQueueTokenResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            isValid = true
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
        log.info("Redis 토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        // 활성 상태면 활성 큐에서 제거
        if (token.isActive()) {
            queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        }

        // 토큰 만료 처리
        val expiredToken = token.copy(
            tokenStatus = QueueTokenStatus.EXPIRED,
            expiresAt = java.time.LocalDateTime.now()
        )
        queueTokenRepository.save(expiredToken)

        log.info("토큰 만료 완료: tokenId=${command.tokenId}")
        return ExpireQueueTokenResult(success = true)
    }

    override fun completeToken(command: CompleteQueueTokenCommand): CompleteQueueTokenResult {
        log.info("Redis 토큰 완료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        // 활성 상태면 활성 큐에서 제거
        if (token.isActive()) {
            queueManagementService.removeFromAllQueues(token.concertId, token.userId)
        }

        // 토큰 완료 처리
        val completedToken = token.copy(
            tokenStatus = QueueTokenStatus.COMPLETED,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5) // 5분 후 정리
        )
        queueTokenRepository.save(completedToken)

        log.info("토큰 완료 처리 완료: tokenId=${command.tokenId}")
        return CompleteQueueTokenResult(success = true)
    }

    /**
     * 토큰의 현재 위치 계산
     */
    private fun calculatePosition(token: QueueToken): Int {
        return if (token.isWaiting()) {
            val rank = queueManagementService.getWaitingPosition(token.concertId, token.userId)
            redisQueueDomainService.calculateWaitingPosition(rank)
        } else 0
    }

    /**
     * 예상 대기 시간 계산 (분 단위)
     */
    private fun calculateEstimatedWaitTime(position: Int): Int {
        return if (position > 0) {
            // 10명당 1분 대기로 가정
            (position / 10).coerceAtLeast(1) // 최소 1분
        } else 0
    }
}

// ========== QueueToken 확장 함수들 ==========

private fun QueueToken.isWaiting(): Boolean = tokenStatus == QueueTokenStatus.WAITING
private fun QueueToken.isActive(): Boolean = tokenStatus == QueueTokenStatus.ACTIVE
private fun QueueToken.isExpired(): Boolean =
    tokenStatus == QueueTokenStatus.EXPIRED ||
            (expiresAt != null && expiresAt.isBefore(java.time.LocalDateTime.now()))