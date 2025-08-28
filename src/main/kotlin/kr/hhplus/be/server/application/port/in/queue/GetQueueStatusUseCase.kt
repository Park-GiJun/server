package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.GetQueueStatusResult

interface GetQueueStatusUseCase {
    fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult
}