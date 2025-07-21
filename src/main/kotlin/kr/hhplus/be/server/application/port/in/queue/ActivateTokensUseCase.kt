package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.command.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.result.ActivateTokensResult

interface ActivateTokensUseCase {
    fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult
}