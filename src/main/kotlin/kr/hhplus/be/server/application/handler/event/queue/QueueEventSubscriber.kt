package kr.hhplus.be.server.application.handler.event.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPublisher
import kr.hhplus.be.server.domain.queue.event.QueueEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class QueueEventSubscriber(
    private val queueEventPublisher: QueueEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val activeCounters = ConcurrentHashMap<Long, AtomicInteger>()

    @KafkaListener(
        topics = ["concert.queue.events"],
        groupId = "queue-event-processor"
    )
    fun subscribe(record: ConsumerRecord<Long, QueueEvent>) {
        when (val event = record.value()) {
            is QueueEvent.QueueCompleted -> handleQueueCompleted(event)
            is QueueEvent.QueueExpired -> handleQueueExpired(event)
            is QueueEvent.QueueActivated -> handleQueueActivated(event)
            else -> {}
        }
    }

    private fun handleQueueActivated(event: QueueEvent.QueueActivated) {
        val activeCount = activeCounters.computeIfAbsent(event.concertId) { AtomicInteger(0) }
        activeCount.incrementAndGet()

        log.info("Token activated: ${event.tokenId}")
        notifyClient(event.tokenId, "ACTIVE", 0)
    }

    private fun handleQueueCompleted(event: QueueEvent.QueueCompleted) {
        val activeCount = activeCounters.get(event.concertId)
        val currentCount = activeCount?.decrementAndGet() ?: 0

        log.info("Token completed: ${event.tokenId}, active count: $currentCount")

        notifyClient(event.tokenId, "COMPLETED", 0)
    }

    private fun handleQueueExpired(event: QueueEvent.QueueExpired) {
        val activeCount = activeCounters.get(event.concertId)
        activeCount?.decrementAndGet()

        log.info("Token expired: ${event.tokenId}")
        notifyClient(event.tokenId, "EXPIRED", 0)
    }

    private fun notifyClient(tokenId: String, status: String, position: Int) {
        log.info("Notify client: tokenId=$tokenId, status=$status, position=$position")
    }
}
