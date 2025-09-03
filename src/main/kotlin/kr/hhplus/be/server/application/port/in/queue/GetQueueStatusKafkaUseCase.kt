package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.event.queue.query.GetQueueStatusKafkaQuery
import kr.hhplus.be.server.application.dto.event.queue.result.GetQueueStatusKafkaResult

interface GetQueueStatusKafkaUseCase {
    fun getQueueStatus(query: GetQueueStatusKafkaQuery): GetQueueStatusKafkaResult
}