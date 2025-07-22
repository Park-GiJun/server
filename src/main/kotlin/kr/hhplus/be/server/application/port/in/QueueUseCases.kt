package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.queue.command.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.command.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.ActivateTokensResult
import kr.hhplus.be.server.application.dto.queue.result.QueueStatusResult
import kr.hhplus.be.server.application.dto.queue.result.ValidateTokenResult

interface ActivateTokensUseCase {
    fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult
}

interface CompleteTokenUseCase {
    fun completeToken(command: CompleteTokenCommand): Boolean
}

interface ExpireTokenUseCase {
    fun expireToken(command: ExpireTokenCommand): Boolean
}

interface GenerateTokenUseCase {
    fun generateToken(command: GenerateTokenCommand): String
}

interface GetQueueStatusUseCase {
    fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult
}

interface ValidateTokenUseCase {
    fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult
    fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult
}

