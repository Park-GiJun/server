package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.ActivateTokensCommand
import kr.hhplus.be.server.application.dto.queue.ActivateTokensResult
import kr.hhplus.be.server.application.dto.queue.CompleteTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.application.mapper.QueueMapper
import kr.hhplus.be.server.application.port.`in`.ActivateTokensUseCase
import kr.hhplus.be.server.application.port.`in`.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase,
    CompleteTokenUseCase, ActivateTokensUseCase {
    
    private val queueDomainService = QueueDomainService()

    override fun generateToken(command: GenerateTokenCommand): String {
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

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
            ?: throw QueueTokenNotFoundException(command.tokenId)

        if (token.isExpired()) {
            val expiredToken = token.expire()
            queueTokenRepository.save(expiredToken)
            throw InvalidTokenStatusException(token.tokenStatus, kr.hhplus.be.server.domain.queue.QueueTokenStatus.ACTIVE)
        }

        val validatedToken = queueDomainService.validateActiveToken(token)

        return QueueMapper.toValidateResult(validatedToken, true)
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        val tokenResult = validateActiveToken(command)

        command.concertId?.let { concertId ->
            if (tokenResult.concertId != concertId) {
                throw InvalidTokenException("Token concert ID mismatch. Expected: $concertId, Actual: ${tokenResult.concertId}")
            }
        }

        return tokenResult
    }

    override fun expireToken(command: ExpireTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val expiredToken = token.expire()
        queueTokenRepository.save(expiredToken)
        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val completedToken = token.complete()
        queueTokenRepository.save(completedToken)
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