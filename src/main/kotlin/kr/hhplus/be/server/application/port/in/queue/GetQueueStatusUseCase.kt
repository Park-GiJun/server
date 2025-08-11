package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusResult

interface GetQueueStatusUseCase {
    fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult
}