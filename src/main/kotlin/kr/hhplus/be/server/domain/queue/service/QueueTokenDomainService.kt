package kr.hhplus.be.server.domain.queue.service

import kr.hhplus.be.server.application.dto.queue.result.ValidateQueueTokenResult
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class QueueTokenDomainService {

    fun createActiveToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = generateTokenId(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            enteredAt = LocalDateTime.now()
        )
    }

    fun createCompletedToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = generateTokenId(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.COMPLETED,
            createdAt = LocalDateTime.now(),
            enteredAt = LocalDateTime.now()
        )
    }

    fun createFromValidationResult(tokenResult: ValidateQueueTokenResult, tokenStatus: QueueTokenStatus): QueueToken {
        return QueueToken(
            queueTokenId = tokenResult.tokenId,
            userId = tokenResult.userId,
            concertId = tokenResult.concertId,
            tokenStatus = tokenStatus,
            createdAt = tokenResult.createdAt ?: LocalDateTime.now(),
            enteredAt = tokenResult.enteredAt ?: LocalDateTime.now()
        )
    }

    private fun generateTokenId(): String {
        return "qt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
}