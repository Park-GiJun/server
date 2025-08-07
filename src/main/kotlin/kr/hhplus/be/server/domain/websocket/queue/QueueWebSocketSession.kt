package kr.hhplus.be.server.domain.websocket.queue

data class QueueWebSocketSession(
    val sessionId: String,
    val tokenId: String,
    val userId: String,
    val concertId: Long,
    val connectedAt: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean = tokenId.isNotBlank() && userId.isNotBlank() && concertId > 0
}
