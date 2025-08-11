package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.port.`in`.queue.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.service.RedisQueueDomainService
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class RedisQueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val redisQueueDomainService: RedisQueueDomainService,
    private val queueManagementService: RedisQueueManagementService
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase, CompleteTokenUseCase {

    private val log = LoggerFactory.getLogger(RedisQueueCommandService::class.java)

    override fun generateToken(command: GenerateTokenCommand): String {
        log.info("Redis 대기열 토큰 생성: userId=${command.userId}, concertId=${command.concertId}")

        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId, command.concertId
        )

        if (existingToken != null) {
            log.info("기존 토큰 반환: tokenId=${existingToken.queueTokenId}")
            return existingToken.queueTokenId
        }

        val newToken = redisQueueDomainService.createNewQueueToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        log.info("Redis 토큰 생성 완료: tokenId=${savedToken.queueTokenId}")
        return savedToken.queueTokenId
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isExpired()) {
            val expiredToken = token.expire()
            queueTokenRepository.save(expiredToken)
            log.warn("토큰 만료: tokenId=${command.tokenId}")
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        return ValidateTokenResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            isValid = true
        )
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
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

    override fun expireToken(command: ExpireTokenCommand): Boolean {
        log.info("Redis 토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        // 활성 상태면 활성 세트에서 제거
        if (token.isActive()) {
            queueManagementService.deactivateUser(token.concertId, token.userId)
        }

        val expiredToken = token.expire()
        queueTokenRepository.save(expiredToken)

        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        log.info("Redis 토큰 완료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isActive()) {
            queueManagementService.deactivateUser(token.concertId, token.userId)
        }

        val completedToken = token.complete()
        queueTokenRepository.save(completedToken)

        return true
    }
}