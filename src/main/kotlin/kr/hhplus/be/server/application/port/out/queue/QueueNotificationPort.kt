package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueuePositionUpdate

/**
 * 대기열 알림 포트
 * - WebSocket, SSE, Push 등 다양한 구현체 가능
 */
interface QueueNotificationPort {

    /**
     * 대기열 진입 알림
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param position 초기 대기 위치
     * @param estimatedWaitTime 예상 대기 시간 (분)
     */
    suspend fun notifyQueueEntry(
        tokenId: String,
        userId: String,
        concertId: Long,
        position: Long,
        estimatedWaitTime: Int
    )

    /**
     * 대기열 위치 업데이트 알림
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param newPosition 새로운 위치
     * @param estimatedWaitTime 예상 대기 시간 (분)
     */
    suspend fun notifyPositionUpdate(
        tokenId: String,
        userId: String,
        concertId: Long,
        newPosition: Long,
        estimatedWaitTime: Int
    )

    /**
     * 토큰 활성화 알림 (예약 가능)
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     */
    suspend fun notifyTokenActivated(
        tokenId: String,
        userId: String,
        concertId: Long
    )

    /**
     * 토큰 만료 알림
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param reason 만료 사유
     */
    suspend fun notifyTokenExpired(
        tokenId: String,
        userId: String,
        concertId: Long,
        reason: String
    )

    /**
     * 배치 위치 업데이트 알림 (성능 최적화)
     * @param concertId 콘서트 ID
     * @param updates 위치 업데이트 목록
     */
    suspend fun notifyBatchPositionUpdates(
        concertId: Long,
        updates: List<QueuePositionUpdate>
    )
}

/**
 * 대기열 위치 업데이트 정보
 */
data class QueuePositionUpdate(
    val tokenId: String,
    val userId: String,
    val newPosition: Long,
    val estimatedWaitTime: Int
)