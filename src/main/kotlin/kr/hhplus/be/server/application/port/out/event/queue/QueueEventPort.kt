package kr.hhplus.be.server.application.port.out.event.queue

/**
 * 대기열 이벤트 포트
 */
interface QueueEventPort {

    fun publishQueueEntered(
        tokenId: String,
        userId: String,
        concertId: Long,
        position: Long,
        estimatedWaitTime: Int
    )

    suspend fun publishPositionUpdated(
        tokenId: String,
        userId: String,
        concertId: Long,
        newPosition: Long,
        estimatedWaitTime: Int
    )

    fun publishTokenActivated(
        tokenId: String,
        userId: String,
        concertId: Long
    )

    suspend fun publishTokenExpired(
        tokenId: String,
        userId: String,
        concertId: Long,
        reason: String
    )

    suspend fun publishBatchPositionUpdates(
        concertId: Long,
        updates: List<QueuePositionUpdate>
    )
}

data class QueuePositionUpdate(
    val tokenId: String,
    val userId: String,
    val newPosition: Long,
    val estimatedWaitTime: Int
)