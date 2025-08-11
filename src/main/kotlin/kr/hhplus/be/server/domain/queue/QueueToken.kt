package kr.hhplus.be.server.domain.queue

import java.time.LocalDateTime
import java.util.*

/**
 * 대기열 토큰 도메인 엔티티
 * - 순수한 도메인 로직만 포함
 */
data class QueueToken(
    val tokenId: String = UUID.randomUUID().toString(),
    val userId: String,
    val concertId: Long,
    val status: QueueTokenStatus = QueueTokenStatus.WAITING,
    val enteredAt: LocalDateTime = LocalDateTime.now(),
    val activatedAt: LocalDateTime? = null,
    val expiredAt: LocalDateTime? = null
) {

    companion object {
        /**
         * 새로운 대기열 토큰 생성
         */
        fun create(userId: String, concertId: Long): QueueToken {
            return QueueToken(
                userId = userId,
                concertId = concertId,
                status = QueueTokenStatus.WAITING,
                enteredAt = LocalDateTime.now()
            )
        }
    }

    /**
     * 토큰 활성화 (예약 가능 상태로 전환)
     */
    fun activate(): QueueToken {
        return this.copy(
            status = QueueTokenStatus.ACTIVE,
            activatedAt = LocalDateTime.now()
        )
    }

    /**
     * 토큰 완료 (예약 완료)
     */
    fun complete(): QueueToken {
        return this.copy(
            status = QueueTokenStatus.COMPLETED
        )
    }

    /**
     * 토큰 만료
     */
    fun expire(): QueueToken {
        return this.copy(
            status = QueueTokenStatus.EXPIRED,
            expiredAt = LocalDateTime.now()
        )
    }

    /**
     * 대기 중인지 확인
     */
    fun isWaiting(): Boolean = status == QueueTokenStatus.WAITING

    /**
     * 활성 상태인지 확인
     */
    fun isActive(): Boolean = status == QueueTokenStatus.ACTIVE

    /**
     * 토큰이 유효한지 확인
     */
    fun isValid(): Boolean = status in listOf(QueueTokenStatus.WAITING, QueueTokenStatus.ACTIVE)

    /**
     * 도메인 규칙 검증
     */
    fun validate() {
        require(userId.isNotBlank()) { "사용자 ID는 필수입니다" }
        require(concertId > 0) { "유효한 콘서트 ID가 필요합니다" }
        require(tokenId.isNotBlank()) { "토큰 ID는 필수입니다" }
    }
}

