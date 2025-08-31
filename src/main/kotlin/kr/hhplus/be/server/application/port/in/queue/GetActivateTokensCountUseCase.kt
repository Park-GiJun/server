package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.query.GetActivateTokensCountQuery
import kr.hhplus.be.server.application.dto.queue.result.GetActivateTokensCountResult

interface GetActivateTokensCountUseCase {
    fun getActivateTokensCountByQueue(query: GetActivateTokensCountQuery): GetActivateTokensCountResult
}