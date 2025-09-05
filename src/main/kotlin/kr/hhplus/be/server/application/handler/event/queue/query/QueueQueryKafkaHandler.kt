package kr.hhplus.be.server.application.handler.event.queue.query

import kr.hhplus.be.server.application.dto.event.queue.query.GetQueueStatusKafkaQuery
import kr.hhplus.be.server.application.dto.event.queue.result.GetQueueStatusKafkaResult
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusKafkaUseCase
import kr.hhplus.be.server.application.port.out.event.queue.QueueEventReader
import org.springframework.stereotype.Component

@Component
class QueueQueryKafkaHandler (
    private val queueEventReader: QueueEventReader
) : GetQueueStatusKafkaUseCase {

    override fun getQueueStatus(query: GetQueueStatusKafkaQuery): GetQueueStatusKafkaResult {
        val position = queueEventReader.getQueuePosition(query.tokenId, query.concertId)
        val activeCount = queueEventReader.getActiveCount(query.concertId)
        val waitingCount = queueEventReader.getWaitingCount(query.concertId)

        val status = when {
            position <= 100 -> "ACTIVE"
            position > 100 -> "WAITING"
            else -> "NOT_FOUND"
        }

        return GetQueueStatusKafkaResult(
            tokenId = query.tokenId,
            position = position,
            activeCount = activeCount,
            waitingCount = waitingCount,
            status = status
        )
    }
}