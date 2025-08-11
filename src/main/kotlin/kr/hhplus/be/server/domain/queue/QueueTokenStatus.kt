package kr.hhplus.be.server.domain.queue

/**
 * 대기열 토큰 상태
 */
enum class QueueTokenStatus {
    WAITING,    // 대기 중
    ACTIVE,     // 활성 (예약 가능)
    COMPLETED,  // 완료 (예약 완료)
    EXPIRED     // 만료
}