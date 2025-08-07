package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.*
import kr.hhplus.be.server.application.mapper.QueueMapper
import kr.hhplus.be.server.application.port.`in`.queue.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.service.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase,
    CompleteTokenUseCase {

    private val queueDomainService = QueueDomainService()
    private val log = LoggerFactory.getLogger(QueueCommandService::class.java)

    override fun generateToken(command: GenerateTokenCommand): String {
        log.info("대기열 토큰 생성: userId=${command.userId}, concertId=${command.concertId}")

        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId, command.concertId
        )

        if (existingToken != null) {
            log.info("기존 토큰 반환: tokenId=${existingToken.queueTokenId}")
            return existingToken.queueTokenId
        }

        val newToken = queueDomainService.createNewToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        log.info("새 토큰 생성 완료: tokenId=${savedToken.queueTokenId}")
        return savedToken.queueTokenId
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val validatedOrExpiredToken = queueDomainService.validateTokenAndExpireIfNeeded(token)

        if (validatedOrExpiredToken.isExpired()) {
            queueTokenRepository.save(validatedOrExpiredToken)
            log.warn("토큰 만료: tokenId=${command.tokenId}")
            throw InvalidTokenStatusException(
                token.tokenStatus,
                QueueTokenStatus.ACTIVE
            )
        }

        val validatedToken = queueDomainService.validateActiveToken(validatedOrExpiredToken)
        return QueueMapper.toValidateResult(validatedToken, true)
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        val tokenResult = validateActiveToken(command)

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        queueDomainService.validateTokenForConcert(token, command.concertId)

        return tokenResult
    }

    override fun expireToken(command: ExpireTokenCommand): Boolean {
        log.info("토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val expiredToken = queueDomainService.expireToken(token)
        queueTokenRepository.save(expiredToken)

        return true
    }

    override fun completeToken(command: CompleteTokenCommand): Boolean {
        log.info("토큰 완료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val completedToken = queueDomainService.completeToken(token)
        queueTokenRepository.save(completedToken)
        return true
    }
}