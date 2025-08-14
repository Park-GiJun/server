package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.GetActivateTokensCountQuery
import kr.hhplus.be.server.application.dto.queue.GetActivateTokensCountResult

interface GetActivateTokensCountUseCase {
    fun getActivateTokensCountByQueue(query: GetActivateTokensCountQuery): GetActivateTokensCountResult
}