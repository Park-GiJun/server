package kr.hhplus.be.server.infrastructure.adapter.out.event.kafka.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPublisher
import kr.hhplus.be.server.domain.queue.event.QueueEvent
import kr.hhplus.be.server.domain.queue.service.QueuePartitionStrategy
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaQueueEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<Long, Any>,
    private val partitionStrategy: QueuePartitionStrategy
) : QueueEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: QueueEvent) {
        val partition = partitionStrategy.getPartition(event.concertId)

        val record = ProducerRecord(
            "concert.queue.events",
            partition,
            event.concertId,
            event
        )

        kafkaTemplate.send(record)
            .addCallback(
                { result ->
                    log.debug("Event published successfully: ${event::class.simpleName}, partition=$partition")
                },
                { ex ->
                    log.error("Failed to publish event: ${event::class.simpleName}", ex)
                }
            )
    }

    override fun publishBatch(events: List<QueueEvent>) {
        events.forEach { publish(it) }
    }
}