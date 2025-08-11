package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.dto.queue.result.GetQueueStatusResult

interface GetQueueStatusUseCase {
    suspend fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult
}

