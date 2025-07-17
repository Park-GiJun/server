package kr.hhplus.be.server.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.exception.InvalidTokenException
import kr.hhplus.be.server.exception.InvalidTokenStatusException
import kr.hhplus.be.server.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.exception.UserNotFoundException
import kr.hhplus.be.server.repository.mock.MockQueueTokenRepository
import kr.hhplus.be.server.repository.mock.MockUserRepository
import kr.hhplus.be.server.util.JwtQueueTokenUtil
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class QueueService(
    private val queueTokenRepository: MockQueueTokenRepository,
    private val userRepository: MockUserRepository,
    private val jwtQueueTokenUtil: JwtQueueTokenUtil
) {


    fun generateQueueToken(userId: String, concertId: Long): String {
        validateUser(userId)

        val newToken = createNewToken(userId, concertId)
        val savedToken = queueTokenRepository.save(newToken)

        val position = calculateWaitingPosition(savedToken)

        return jwtQueueTokenUtil.generateToken(
            userId = userId,
            concertId = concertId,
            position = position,
            status = QueueTokenStatus.WAITING
        )
    }

    fun getQueueStatus(tokenId: String): String {
        if (!jwtQueueTokenUtil.validateToken(tokenId)) {
            throw InvalidTokenException("Invalid JWT token")
        }

        if (jwtQueueTokenUtil.isTokenExpired(tokenId)) {
            throw InvalidTokenStatusException("Token has expired")
        }

        val userId = jwtQueueTokenUtil.getUserIdFromToken(tokenId)
            ?: throw InvalidTokenException("Cannot extract user ID from token")

        val concertId = jwtQueueTokenUtil.getConcertIdFromToken(tokenId)
            ?: throw InvalidTokenException("Cannot extract concert ID from token")

        val dbToken = queueTokenRepository.findByUserIdAndConcertId(userId, concertId)
            ?: throw QueueTokenNotFoundException("Queue token not found in database")

        return when {
            dbToken.isExpired() -> throw InvalidTokenStatusException("Token has expired")
            dbToken.isActive() -> {
                jwtQueueTokenUtil.generateToken(
                    userId = userId,
                    concertId = concertId,
                    position = 0,
                    status = QueueTokenStatus.ACTIVE
                )
            }
            dbToken.isWaiting() -> {
                val position = calculateWaitingPosition(dbToken)
                jwtQueueTokenUtil.generateToken(
                    userId = userId,
                    concertId = concertId,
                    position = position,
                    status = QueueTokenStatus.WAITING
                )
            }
            else -> throw InvalidTokenStatusException("Invalid token status: ${dbToken.tokenStatus}")
        }
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): List<QueueToken> {
        val tokensToActivate = queueTokenRepository
            .findWaitingTokensByConcertIdOrderByEnteredAt(concertId)
            .take(count)

        return tokensToActivate.map { token ->
            val activatedToken = token.activate()
            queueTokenRepository.save(activatedToken)
        }
    }

    fun expireToken(tokenId: String): Boolean {
        val userId = jwtQueueTokenUtil.getUserIdFromToken(tokenId)
            ?: throw InvalidTokenStatusException("Cannot extract user ID from token")

        val concertId = jwtQueueTokenUtil.getConcertIdFromToken(tokenId)
            ?: throw InvalidTokenStatusException("Cannot extract concert ID from token")

        val token = queueTokenRepository.findByUserIdAndConcertId(userId, concertId)
            ?: throw QueueTokenNotFoundException("Queue token not found")

        val expiredToken = token.expire()
        queueTokenRepository.save(expiredToken)
        return true
    }

    fun parseTokenForValidation(jwtToken: String): kr.hhplus.be.server.dto.QueueTokenStatusRequest {
        return jwtQueueTokenUtil.parseToken(jwtToken)
    }

    private fun validateUser(userId: String) {
        userRepository.findByUserId(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun createNewToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueToken = UUID.randomUUID().toString(),
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
}