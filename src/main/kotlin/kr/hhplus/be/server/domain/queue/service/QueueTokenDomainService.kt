package kr.hhplus.be.server.domain.queue

import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 큐 토큰 도메인 서비스
 * - 큐 토큰과 관련된 핵심 비즈니스 로직을 담당
 * - 기존 ReservationDomainService 패턴을 따름
 */
@Component
class QueueTokenDomainService {

    /**
     * ACTIVE 상태의 토큰 생성
     *
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @return 생성된 QueueToken (ACTIVE 상태)
     */
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

    /**
     * COMPLETED 상태의 토큰 생성 (예약/결제 완료)
     *
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @return 생성된 QueueToken (COMPLETED 상태)
     */
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

    /**
     * EXPIRED 상태의 토큰 생성 (만료)
     *
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @return 생성된 QueueToken (EXPIRED 상태)
     */
    fun createExpiredToken(userId: String, concertId: Long): QueueToken {
        return QueueToken(
            queueTokenId = generateTokenId(),
            userId = userId,
            concertId = concertId,
            tokenStatus = QueueTokenStatus.EXPIRED,
            createdAt = LocalDateTime.now(),
            enteredAt = LocalDateTime.now()
        )
    }

    /**
     * 토큰 검증 결과로부터 QueueToken 도메인 객체 생성
     * - ReservationCommandService에서 사용
     *
     * @param tokenResult 토큰 검증 결과 (애플리케이션 계층의 DTO)
     * @return QueueToken 도메인 객체 (ACTIVE 상태로 간주)
     */
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

    /**
     * 토큰 ID 생성
     */
    private fun generateTokenId(): String {
        return "qt_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}"
    }
}