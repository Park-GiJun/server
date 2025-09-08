package kr.hhplus.be.server.infrastructure.config.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    companion object {
        const val QUEUE_EVENT_TOPIC = "concert.queue.events"
        const val QUEUE_STATE_TOPIC = "concert.queue.state"
        const val PARTITION_COUNT = 10
    }

    @Bean
    fun queueEventTopic(): NewTopic {
        return TopicBuilder.name(QUEUE_EVENT_TOPIC)
            .partitions(PARTITION_COUNT)
            .replicas(1)
            .config("retention.ms", "9800000")
            .build()
    }

    @Bean
    fun queueStateTopic(): NewTopic {
        return TopicBuilder.name(QUEUE_STATE_TOPIC)
            .partitions(PARTITION_COUNT)
            .replicas(1)
            .config("cleanup.policy", "compact")
            .config("min.compaction.lag.ms", "60000")
            .config("segment.ms", "300000")
            .build()
    }
}