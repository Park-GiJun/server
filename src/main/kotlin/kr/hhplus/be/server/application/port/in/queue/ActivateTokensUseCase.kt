package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.ActivateTokensResult

interface ActivateTokensUseCase {
    fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult
}