package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.command.*
import kr.hhplus.be.server.application.dto.result.*
import kr.hhplus.be.server.application.port.`in`.queue.*
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.exception.*
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val queueDomainService: QueueDomainService
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase,
    CompleteTokenUseCase, ActivateTokensUseCase {

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

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: ${command.tokenId}")

        if (token.isExpired()) {
            token.expire()
            queueTokenRepository.save(token)
            throw InvalidTokenStatusException("Token has expired")
        }

        if (!token.isActive()) {
            throw InvalidTokenStatusException("Token is not active. Current status: ${token.tokenStatus}")
        }

        return ValidateTokenResult(
            tokenId = token.queueTokenId,
            userId = token.userId,
            concertId = token.concertId,
            isValid = true
        )
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        val tokenResult = validateActiveToken(command)

        command.concertId?.let { concertId ->
            if (tokenResult.concertId != concertId) {
                throw InvalidTokenException("Token concert ID mismatch")
            }
        }

        return tokenResult
    }

    override fun expireToken(command: ExpireTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: ${command.tokenId}")

        token.expire()
        queueTokenRepository.save(token)
        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException("Token not found: ${command.tokenId}")

        token.complete()
        queueTokenRepository.save(token)
        return true
    }

    override fun activateTokens(command: ActivateTokensCommand): ActivateTokensResult {
        val activatedTokens = queueTokenRepository.activateWaitingTokens(command.concertId, command.count)

        return ActivateTokensResult(
            activatedCount = activatedTokens.size,
            tokenIds = activatedTokens.map { it.queueTokenId }
        )
    }
}