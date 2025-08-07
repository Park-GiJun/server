package kr.hhplus.be.server.application.port.out.websocket.queue

import kr.hhplus.be.server.domain.websocket.queue.QueueWebSocketSession

interface QueueWebSocketSessionPort {
    fun addSession(session: QueueWebSocketSession)
    fun removeSession(tokenId: String)
    fun getSession(tokenId: String): QueueWebSocketSession?
    fun getActiveSessionCount(concertId: Long): Int
    fun getAllSessions(): List<QueueWebSocketSession>
}