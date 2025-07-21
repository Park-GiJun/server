package kr.hhplus.be.server.domain.queue

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

    fun calculateWaitingPosition(waitingTokensBeforeUser: Int): Int {
        return (waitingTokensBeforeUser + 1)
    }
}