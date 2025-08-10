package kr.hhplus.be.server.domain.queue.service

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.TokenExpiredException
import java.time.LocalDateTime
import java.util.UUID

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

    fun validateTokenAndExpireIfNeeded(token: QueueToken): QueueToken {
        if (token.isExpired()) {
            return token.expire()
        }
        return token
    }

    fun validateTokenForConcert(token: QueueToken, expectedConcertId: Long?) {
        expectedConcertId?.let { concertId ->
            if (token.concertId != concertId) {
                throw InvalidTokenException("Token concert ID mismatch. Expected: $concertId, Actual: ${token.concertId}")
            }
        }
    }

    fun calculateWaitingPosition(waitingTokensBeforeUser: Int): Int {
        return (waitingTokensBeforeUser + 1)
    }

    fun expireToken(token: QueueToken): QueueToken {
        return token.expire()
    }

    fun completeToken(token: QueueToken): QueueToken {
        return token.complete()
    }

    fun calculatePositionByIndex(tokens: List<QueueToken>): Map<String, Int> {
        return tokens.mapIndexed { index, token ->
            token.queueTokenId to (index + 1)
        }.toMap()
    }

    fun findChangedTokenIds(
        oldPositions: Map<String, Int>,
        newPositions: Map<String, Int>
    ): Set<String> {
        return newPositions.keys.filter { tokenId ->
            oldPositions[tokenId] != newPositions[tokenId]
        }.toSet()
    }
}