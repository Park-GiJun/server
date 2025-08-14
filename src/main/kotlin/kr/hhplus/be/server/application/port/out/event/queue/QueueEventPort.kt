package kr.hhplus.be.server.application.port.out.event.queue

/**
 * 대기열 이벤트 포트
 */
interface QueueEventPort {

    /**
     * 대기열 진입 이벤트 발행
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param position 초기 대기 위치
     * @param estimatedWaitTime 예상 대기 시간 (분)
     */
    fun publishQueueEntered(
        tokenId: String,
        userId: String,
        concertId: Long,
        position: Long,
        estimatedWaitTime: Int
    )

    /**
     * 대기열 위치 업데이트 이벤트 발행
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param newPosition 새로운 위치
     * @param estimatedWaitTime 예상 대기 시간 (분)
     */
    suspend fun publishPositionUpdated(
        tokenId: String,
        userId: String,
        concertId: Long,
        newPosition: Long,
        estimatedWaitTime: Int
    )

    /**
     * 토큰 활성화 이벤트 발행 (예약 가능)
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     */
    fun publishTokenActivated(
        tokenId: String,
        userId: String,
        concertId: Long
    )

    /**
     * 토큰 만료 이벤트 발행
     * @param tokenId 토큰 ID
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @param reason 만료 사유
     */
    suspend fun publishTokenExpired(
        tokenId: String,
        userId: String,
        concertId: Long,
        reason: String
    )

    /**
     * 배치 위치 업데이트 이벤트 발행
     * @param concertId 콘서트 ID
     * @param updates 위치 업데이트 목록
     */
    suspend fun publishBatchPositionUpdates(
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