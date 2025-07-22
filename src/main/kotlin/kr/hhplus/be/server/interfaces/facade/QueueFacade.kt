package kr.hhplus.be.server.interfaces.facade

import kr.hhplus.be.server.application.port.`in`.queue.*
import kr.hhplus.be.server.application.dto.queue.command.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.command.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.ActivateTokensResult
import kr.hhplus.be.server.application.dto.queue.result.QueueStatusResult
import kr.hhplus.be.server.application.dto.queue.result.ValidateTokenResult
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val generateTokenUseCase: GenerateTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val activateTokensUseCase: ActivateTokensUseCase
) {

    fun generateToken(command: GenerateTokenCommand): String {
        return generateTokenUseCase.generateToken(command)
    }

    fun getQueueStatus(query: GetQueueStatusQuery): QueueStatusResult {
        return getQueueStatusUseCase.getQueueStatus(query)
    }

    fun validateActiveToken(tokenId: String): ValidateTokenResult {
        val command = ValidateTokenCommand(tokenId)
        return validateTokenUseCase.validateActiveToken(command)
    }

    fun validateActiveTokenForConcert(tokenId: String, concertId: Long): ValidateTokenResult {
        val command = ValidateTokenCommand(tokenId, concertId)
        return validateTokenUseCase.validateActiveTokenForConcert(command)
    }

    fun expireToken(tokenId: String): Boolean {
        val command = ExpireTokenCommand(tokenId)
        return expireTokenUseCase.expireToken(command)
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): ActivateTokensResult {
        val command = ActivateTokensCommand(concertId, count)
        return activateTokensUseCase.activateTokens(command)
    }
}