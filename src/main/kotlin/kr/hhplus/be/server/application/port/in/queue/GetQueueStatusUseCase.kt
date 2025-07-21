package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.result.QueueStatusResult

interface GetQueueStatusUseCase {
    fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult
}
