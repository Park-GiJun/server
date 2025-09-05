package kr.hhplus.be.server.application.port.out.event.queue

import kr.hhplus.be.server.domain.queue.event.QueueEvent

interface QueueEventPublisher {
    fun publish(event: QueueEvent)
    fun publishBatch(events: List<QueueEvent>)
}