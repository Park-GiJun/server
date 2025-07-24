package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.ActivateTokensResult
import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult

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

