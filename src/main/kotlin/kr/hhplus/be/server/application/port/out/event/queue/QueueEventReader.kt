package kr.hhplus.be.server.application.port.out.event.queue

import kr.hhplus.be.server.domain.queue.event.QueueEvent

interface QueueEventReader {
    fun getQueuePosition(tokenId: String, concertId: Long): Int
    fun getActiveCount(concertId: Long): Int
    fun getWaitingCount(concertId: Long): Int
    fun getRecentEvents(concertId: Long, limit: Int): List<QueueEvent>
}
