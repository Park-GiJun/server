package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.dto

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

data class QueueWebSocketConnectRequest(
    val tokenId: String,
    val userId: String,
    val concertId: Long
)

data class QueueWebSocketResponse(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val status: QueueTokenStatus,
    val position: Int,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class QueuePositionUpdateEvent(
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val newPosition: Int,
    val status: QueueTokenStatus
)

data class QueueActivationEvent(
    val tokenId: String,
    val userId: String,
    val concertId: Long
)