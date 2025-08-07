package kr.hhplus.be.server.application.port.out.websocket.queue

import kr.hhplus.be.server.domain.queue.QueueTokenStatus

interface QueueWebSocketMessagePort {
    fun sendMessage(tokenId: String, message: QueueWebSocketMessage): Boolean
    fun sendToMultiple(tokenIds: List<String>, message: QueueWebSocketMessage): Int
}

data class QueueWebSocketMessage(
    val tokenId: String,
    val userId: String = "",
    val concertId: Long,
    val status: QueueTokenStatus,
    val position: Int,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
