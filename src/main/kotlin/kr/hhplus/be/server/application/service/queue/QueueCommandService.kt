package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.command.GenerateTokenCommand
import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.exception.InvalidTokenStatusException
import kr.hhplus.be.server.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.exception.UserNotFoundException
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val queueDomainService: QueueDomainService
) : GenerateTokenUseCase, ValidateTokenUseCase {

    override fun generateToken(command: GenerateTokenCommand): String {
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException("User not found with id: ${command.userId}")

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId,
            command.concertId
        )
        if (existingToken != null) {
            return existingToken.queueTokenId
        }

        val newToken = queueDomainService.createNewToken(command.userId, command.concertId)

        val savedToken = queueTokenRepository.save(newToken)

        return savedToken.queueTokenId
    }

    override fun validateActiveToken(tokenId: String): QueueToken {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        if (token.isExpired()) {
            token.expire()
            queueTokenRepository.save(token)
            throw InvalidTokenStatusException("Token has expired")
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException("Token is not active. Current status: ${token.tokenStatus}")
        }

        return token
    }

    override fun validateActiveTokenForConcert(tokenId: String, concertId: Long): QueueToken {
        val token = validateActiveToken(tokenId)

        return queueDomainService.validateActiveTokenForConcert(token, concertId)
    }

    fun expireToken(tokenId: String): Boolean {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        token.expire()
        queueTokenRepository.save(token)
        return true
    }

    fun completeToken(tokenId: String): Boolean {
        val token = queueTokenRepository.findByTokenId(tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: $tokenId")

        token.complete()
        queueTokenRepository.save(token)
        return true
    }

    fun activateNextTokens(concertId: Long, count: Int = 10): List<QueueToken> {
        return queueTokenRepository.activateWaitingTokens(concertId, count)
    }
}
