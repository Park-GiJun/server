package kr.hhplus.be.server.domain.queue

import kr.hhplus.be.server.exception.InvalidTokenException
import kr.hhplus.be.server.exception.InvalidTokenStatusException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class QueueDomainService {

    fun createNewToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = UUID.randomUUID().toString(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.WAITING,
            enteredAt = LocalDateTime.now()
        )
    }

    fun validateActiveToken(token: QueueToken, userId: String): QueueToken {
        if (token.userId != userId) {
            throw InvalidTokenException("Token user mismatch")
        }

        if (token.isExpired()) {
            throw InvalidTokenStatusException("Token has expired")
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException("Token is not active. Current status: ${token.tokenStatus}")
        }

        return token
    }

    fun validateActiveTokenForConcert(token: QueueToken, concertId: Long): QueueToken {
        if (token.concertId != concertId) {
            throw InvalidTokenException("Token concert ID mismatch")
        }

        return validateActiveToken(token, token.userId)
    }

    fun calculateWaitingPosition(waitingTokensBeforeUser: Long): Int {
        return (waitingTokensBeforeUser + 1).toInt()
    }

    fun calculateEstimatedWaitTime(position: Int): Int {
        if (position <= 0) return 0
        return (position / 10) * 60
    }
}