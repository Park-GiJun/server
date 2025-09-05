package kr.hhplus.be.server.infrastructure.adapter.out.event.kafka.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventReader
import kr.hhplus.be.server.domain.queue.event.QueueEvent
import kr.hhplus.be.server.domain.queue.service.QueuePartitionStrategy
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class KafkaQueueEventReaderAdapter(
    private val partitionStrategy: QueuePartitionStrategy
) : QueueEventReader {

    override fun getQueuePosition(tokenId: String, concertId: Long): Int {
        val partition = partitionStrategy.getPartition(concertId)

        val consumer = createConsumer()
        val topicPartition = TopicPartition("concert.queue.events", partition)
        consumer.assign(listOf(topicPartition))
        consumer.seekToBeginning(listOf(topicPartition))

        var position = 0
        var foundToken = false
        val activeTokens = mutableSetOf<String>()
        val waitingTokens = mutableListOf<String>()

        try {
            val records = consumer.poll(Duration.ofSeconds(1))

            for (record in records) {
                when (val event = record.value()) {
                    is QueueEvent.QueueEntered -> {
                        if (!activeTokens.contains(event.tokenId)) {
                            waitingTokens.add(event.tokenId)
                        }
                    }
                    is QueueEvent.QueueActivated -> {
                        activeTokens.add(event.tokenId)
                        waitingTokens.remove(event.tokenId)
                        if (event.tokenId == tokenId) {
                            return 0
                        }
                    }
                    is QueueEvent.QueueCompleted,
                    is QueueEvent.QueueExpired -> {
                        activeTokens.remove(event.tokenId)
                        waitingTokens.remove(event.tokenId)
                    }
                }
            }

            position = waitingTokens.indexOf(tokenId) + 1

        } finally {
            consumer.close()
        }

        return position
    }

    override fun getActiveCount(concertId: Long): Int {
        val partition = partitionStrategy.getPartition(concertId)

        val consumer = createConsumer()
        val topicPartition = TopicPartition("concert.queue.events", partition)
        consumer.assign(listOf(topicPartition))
        consumer.seekToBeginning(listOf(topicPartition))

        val activeTokens = mutableSetOf<String>()

        try {
            val records = consumer.poll(Duration.ofSeconds(1))

            for (record in records) {
                when (val event = record.value()) {
                    is QueueEvent.QueueActivated -> activeTokens.add(event.tokenId)
                    is QueueEvent.QueueCompleted,
                    is QueueEvent.QueueExpired -> activeTokens.remove(event.tokenId)
                    else -> {}
                }
            }
        } finally {
            consumer.close()
        }

        return activeTokens.size
    }

    override fun getWaitingCount(concertId: Long): Int {
        val partition = partitionStrategy.getPartition(concertId)

        val consumer = createConsumer()
        val topicPartition = TopicPartition("concert.queue.events", partition)
        consumer.assign(listOf(topicPartition))
        consumer.seekToBeginning(listOf(topicPartition))

        val waitingTokens = mutableSetOf<String>()
        val processedTokens = mutableSetOf<String>()

        try {
            val records = consumer.poll(Duration.ofSeconds(1))

            for (record in records) {
                when (val event = record.value()) {
                    is QueueEvent.QueueEntered -> {
                        if (!processedTokens.contains(event.tokenId)) {
                            waitingTokens.add(event.tokenId)
                        }
                    }
                    is QueueEvent.QueueActivated,
                    is QueueEvent.QueueCompleted,
                    is QueueEvent.QueueExpired -> {
                        waitingTokens.remove(event.tokenId)
                        processedTokens.add(event.tokenId)
                    }
                }
            }
        } finally {
            consumer.close()
        }

        return waitingTokens.size
    }

    override fun getRecentEvents(concertId: Long, limit: Int): List<QueueEvent> {
        val partition = partitionStrategy.getPartition(concertId)

        val consumer = createConsumer()
        val topicPartition = TopicPartition("concert.queue.events", partition)
        consumer.assign(listOf(topicPartition))

        val endOffset = consumer.endOffsets(listOf(topicPartition))[topicPartition] ?: 0L
        val startOffset = (endOffset - limit).coerceAtLeast(0)
        consumer.seek(topicPartition, startOffset)

        val events = mutableListOf<QueueEvent>()

        try {
            val records = consumer.poll(Duration.ofSeconds(1))
            records.forEach { record ->
                events.add(record.value() as QueueEvent)
            }
        } finally {
            consumer.close()
        }

        return events.takeLast(limit)
    }

    private fun createConsumer(): KafkaConsumer<Long, QueueEvent> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.LongDeserializer")
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.springframework.kafka.support.serializer.JsonDeserializer")
            put(ConsumerConfig.GROUP_ID_CONFIG, "queue-reader-${UUID.randomUUID()}")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
            put("spring.json.trusted.packages", "kr.hhplus.be.server.domain.queue.event")
        }
        return KafkaConsumer(props)
    }
}
