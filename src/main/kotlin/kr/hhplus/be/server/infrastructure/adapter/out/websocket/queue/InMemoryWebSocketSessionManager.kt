package kr.hhplus.be.server.infrastructure.adapter.out.websocket.queue

import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketSessionPort
import kr.hhplus.be.server.domain.websocket.queue.QueueWebSocketSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryWebSocketSessionManager : QueueWebSocketSessionPort {

    private val log = LoggerFactory.getLogger(InMemoryWebSocketSessionManager::class.java)

    private val sessions = ConcurrentHashMap<String, QueueWebSocketSession>()

    private val concertSessions = ConcurrentHashMap<Long, MutableSet<String>>()

    override fun addSession(session: QueueWebSocketSession) {
        if (!session.isValid()) {
            log.warn("유효하지 않은 세션: $session")
            return
        }

        sessions[session.tokenId] = session
        concertSessions.computeIfAbsent(session.concertId) { mutableSetOf() }.add(session.tokenId)

        log.info("WebSocket 세션 추가: tokenId=${session.tokenId}, concertId=${session.concertId}")
    }

    override fun removeSession(tokenId: String) {
        val session = sessions.remove(tokenId)
        session?.let {
            concertSessions[it.concertId]?.remove(tokenId)
            if (concertSessions[it.concertId]?.isEmpty() == true) {
                concertSessions.remove(it.concertId)
            }
            log.info("WebSocket 세션 제거: tokenId=$tokenId")
        }
    }

    override fun getSession(tokenId: String): QueueWebSocketSession? {
        return sessions[tokenId]
    }

    override fun getActiveSessionCount(concertId: Long): Int {
        return concertSessions[concertId]?.size ?: 0
    }

    override fun getAllSessions(): List<QueueWebSocketSession> {
        return sessions.values.toList()
    }
}