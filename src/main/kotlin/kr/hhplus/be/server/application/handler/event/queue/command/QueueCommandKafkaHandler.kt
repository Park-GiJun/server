package kr.hhplus.be.server.application.handler.event.queue.command

import kr.hhplus.be.server.application.dto.event.queue.command.EnterQueueCommand
import kr.hhplus.be.server.application.dto.event.queue.result.EnterQueueResult
import kr.hhplus.be.server.application.port.`in`.queue.EnterQueueUseCase
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventPublisher
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventReader
import kr.hhplus.be.server.domain.queue.event.QueueEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueCommandKafkaHandler (
    private val queueEventPublisher: QueueEventPublisher,
    private val queueEventReader: QueueEventReader
) : EnterQueueUseCase {

    private val MAX_ACTIVE = 100

    override fun enterQueue(command: EnterQueueCommand): EnterQueueResult {
        val tokenId = UUID.randomUUID().toString()
        val activeCount = queueEventReader.getActiveCount(command.concertId)
        val event = if (activeCount < MAX_ACTIVE) {
            QueueEvent.QueueActivated(
                tokenId = tokenId,
                userId = command.userId,
                concertId = command.concertId
            )
        } else {
            QueueEvent.QueueEntered(
                tokenId = tokenId,
                userId = command.userId,
                concertId = command.concertId
            )
        }
        queueEventPublisher.publish(event)
        val status = if (event is QueueEvent.QueueActivated) "ACTIVE" else "WAITING"
        val position = if (status == "WAITING") {
            queueEventReader.getWaitingCount(command.concertId)
        } else 0
        return EnterQueueResult(
            tokenId = tokenId,
            position = position,
            status = status
        )
    }
}