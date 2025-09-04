package kr.hhplus.be.server.application.handler.event.queue

import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPort
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPublisher
import kr.hhplus.be.server.domain.queue.event.QueueEvent
import kr.hhplus.be.server.domain.queue.service.QueuePartitionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueEventHandler(
    private val queueEventPublisher: QueueEventPublisher,
    private val queueEventSubscriber: QueueEventSubscriber,
    private val partitionStrategy: QueuePartitionStrategy
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enterQueue(userId: Long, concertId: Long): String {
        val tokenId = UUID.randomUUID().toString()

        val event = QueueEvent.QueueEntered(
            tokenId = tokenId,
            userId = userId,
            concertId = concertId
        )

        queueEventPublisher.publish(event)

        return tokenId
    }

    fun activateToken(tokenId: String, userId: Long, concertId: Long) {
        val event = QueueEvent.QueueActivated(
            tokenId = tokenId,
            userId = userId,
            concertId = concertId
        )

        queueEventPublisher.publish(event)
        log.info("Queue activated event published: tokenId=$tokenId")
    }

    fun completeToken(tokenId: String, userId: Long, concertId: Long) {
        val event = QueueEvent.QueueCompleted(
            tokenId = tokenId,
            userId = userId,
            concertId = concertId
        )

        queueEventPublisher.publish(event)
        log.info("Queue completed event published: tokenId=$tokenId")
    }

    fun getQueuePosition(tokenId: String, concertId: Long): Int {
        return queueKafkaEventPort.getQueuePosition(tokenId, concertId)
    }
}
