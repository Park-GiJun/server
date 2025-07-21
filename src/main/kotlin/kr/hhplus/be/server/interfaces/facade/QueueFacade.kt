package kr.hhplus.be.server.interfaces.facade

import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.service.queue.QueueCommandService
import kr.hhplus.be.server.application.dto.command.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.result.QueueStatusResult
import kr.hhplus.be.server.domain.queue.QueueToken
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val generateTokenUseCase: GenerateTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val validateTokenUseCase: ValidateTokenUseCase,

    private val queueCommandService: QueueCommandService
) {

    fun generateToken(userId: String, concertId: Long): String {
        val command = GenerateTokenCommand(userId, concertId)
        return generateTokenUseCase.generateToken(command)
    }

    fun getQueueStatus(tokenId: String): QueueStatusResult {
        val query = GetQueueStatusQuery(tokenId)
        return getQueueStatusUseCase.getQueueStatus(query)
    }

    fun validateActiveToken(tokenId: String): QueueToken {
        return validateTokenUseCase.validateActiveToken(tokenId)
    }

    fun validateActiveTokenForConcert(tokenId: String, concertId: Long): QueueToken {
        return validateTokenUseCase.validateActiveTokenForConcert(tokenId, concertId)
    }

    fun expireToken(tokenId: String): Boolean {
        return queueCommandService.expireToken(tokenId)
    }

    fun completeToken(tokenId: String): Boolean {
        return queueCommandService.completeToken(tokenId)
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): List<QueueToken> {
        return queueCommandService.activateNextTokens(concertId, count)
    }
}