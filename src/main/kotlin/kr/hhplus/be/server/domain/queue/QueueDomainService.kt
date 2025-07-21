package kr.hhplus.be.server.domain.queue

import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.TokenExpiredException
import java.time.LocalDateTime
import java.util.*

class QueueDomainService {

    fun createNewToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = UUID.randomUUID().toString(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.WAITING,
            enteredAt = LocalDateTime.now(),
            expiresAt = LocalDateTime.now().plusHours(1)
        )
    }

    fun validateActiveToken(token: QueueToken): QueueToken {
        if (token.isExpired()) {
            throw TokenExpiredException(token.queueTokenId)
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException(token.tokenStatus, QueueTokenStatus.ACTIVE)
        }

        return token
    }

    fun validateActiveTokenForConcert(token: QueueToken, concertId: Long): QueueToken {
        if (token.concertId != concertId) {
            throw InvalidTokenException("Token concert ID mismatch. Expected: $concertId, Actual: ${token.concertId}")
        }

        return validateActiveToken(token)
    }

    fun calculateWaitingPosition(waitingTokensBeforeUser: Int): Int {
        return (waitingTokensBeforeUser + 1).toInt()
    }

    fun validateTokenForUser(token: QueueToken, requestUserId: String): QueueToken {
        if (token.userId != requestUserId) {
            throw InvalidTokenException("Token user mismatch. Expected: $requestUserId, Actual: ${token.userId}")
        }
        return token
    }
}