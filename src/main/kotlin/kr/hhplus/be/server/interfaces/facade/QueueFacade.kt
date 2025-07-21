package kr.hhplus.be.server.interfaces.facade

import kr.hhplus.be.server.application.port.`in`.queue.*
import kr.hhplus.be.server.application.dto.command.*
import kr.hhplus.be.server.application.dto.query.*
import kr.hhplus.be.server.application.dto.result.*
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val generateTokenUseCase: GenerateTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val completeTokenUseCase: CompleteTokenUseCase,
    private val activateTokensUseCase: ActivateTokensUseCase
) {

    fun generateToken(userId: String, concertId: Long): String {
        val command = GenerateTokenCommand(userId, concertId)
        return generateTokenUseCase.generateToken(command)
    }

    fun getQueueStatus(tokenId: String): QueueStatusResult {
        val query = GetQueueStatusQuery(tokenId)
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

    fun completeToken(tokenId: String): Boolean {
        val command = CompleteTokenCommand(tokenId)
        return completeTokenUseCase.completeToken(command)
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): ActivateTokensResult {
        val command = ActivateTokensCommand(concertId, count)
        return activateTokensUseCase.activateTokens(command)
    }
}