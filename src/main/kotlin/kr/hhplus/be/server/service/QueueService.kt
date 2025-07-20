package kr.hhplus.be.server.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.dto.QueueStatusResponse
import kr.hhplus.be.server.exception.InvalidTokenException
import kr.hhplus.be.server.exception.InvalidTokenStatusException
import kr.hhplus.be.server.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.exception.UserNotFoundException
import kr.hhplus.be.server.repository.mock.MockQueueTokenRepository
import kr.hhplus.be.server.repository.mock.MockUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class QueueService(
    private val queueTokenRepository: MockQueueTokenRepository,
    private val userRepository: MockUserRepository
) {

    private val log = LoggerFactory.getLogger(QueueService::class.java)

    fun generateQueueToken(userId: String, concertId: Long): String {
        validateUser(userId)

        // 기존 활성 토큰 확인
        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(userId, concertId)
        if (existingToken != null) {
            log.info("User $userId already has active token for concert $concertId: ${existingToken.queueTokenId}")
            return existingToken.queueTokenId
        }

        val newToken = createNewToken(userId, concertId)
        val savedToken = queueTokenRepository.save(newToken)

        log.info("Generated new queue token: ${savedToken.queueTokenId} for user $userId, concert $concertId")
        return savedToken.queueTokenId
    }

    fun getQueueStatus(tokenId: String): QueueStatusResponse {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        if (token.isExpired()) {
            token.expire()
            queueTokenRepository.save(token)
            throw InvalidTokenStatusException("Token has expired")
        }

        val position = if (token.isWaiting()) {
            calculateWaitingPosition(token)
        } else 0

        val estimatedWaitTime = calculateEstimatedWaitTime(position)

        return QueueStatusResponse(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            status = token.tokenStatus,
            position = position,
            estimatedWaitTime = estimatedWaitTime
        )
    }

    fun validateActiveToken(tokenId: String): QueueToken {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        if (token.isExpired()) {
            token.expire()
            queueTokenRepository.save(token)
            throw InvalidTokenStatusException("Token has expired")
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException("Token is not active. Current status: ${token.tokenStatus}")
        }

        return token
    }

    fun validateActiveTokenForConcert(tokenId: String, concertId: Long): QueueToken {
        val token = validateActiveToken(tokenId)

        if (token.concertId != concertId) {
            throw InvalidTokenException("Token concert ID (${token.concertId}) does not match requested concert ($concertId)")
        }

        return token
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): List<QueueToken> {
        val activatedTokens = queueTokenRepository.activateWaitingTokens(concertId, count)
        log.info("Activated $count tokens for concert $concertId")
        return activatedTokens
    }

    fun expireToken(tokenId: String): Boolean {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        token.expire()
        queueTokenRepository.save(token)

        log.info("Expired token: $tokenId")
        return true
    }

    fun expireTokenByUser(userId: String, concertId: Long): Boolean {
        val token = queueTokenRepository.findByUserIdAndConcertId(userId, concertId)
            ?: return false

        token.expire()
        queueTokenRepository.save(token)

        log.info("Expired token for user $userId, concert $concertId: ${token.queueTokenId}")
        return true
    }

    fun completeToken(tokenId: String): Boolean {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        token.complete()
        queueTokenRepository.save(token)

        log.info("Completed token: $tokenId")
        return true
    }

    private fun validateUser(userId: String) {
        userRepository.findByUserId(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun createNewToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = UUID.randomUUID().toString(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.WAITING,
            enteredAt = LocalDateTime.now()
        )
    }

    private fun calculateWaitingPosition(token: QueueToken): Int {
        return queueTokenRepository.countWaitingTokensBeforeUser(
            token.userId,
            token.concertId,
            token.enteredAt
        ).toInt() + 1
    }

    private fun calculateEstimatedWaitTime(position: Int): Int {
        if (position <= 0) return 0
        return (position / 10) * 60
    }
}