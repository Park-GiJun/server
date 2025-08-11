package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.*
import kr.hhplus.be.server.application.port.`in`.queue.CompleteTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import kr.hhplus.be.server.application.port.out.queue.QueueNotificationPort
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.queue.exception.InvalidTokenStatusException
import kr.hhplus.be.server.domain.queue.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 기존 QueueCommandService에 Redis 알림 기능을 추가
 * - Redis Pub/Sub 기능만 추가
 */
@Service
@Transactional
class EnhancedQueueCommandService(
    private val queueTokenRepository: QueueTokenRepository,
    private val userRepository: UserRepository,
    private val queueNotificationPort: QueueNotificationPort
) : GenerateTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase, CompleteTokenUseCase {

    private val queueDomainService = QueueDomainService()
    private val log = LoggerFactory.getLogger(EnhancedQueueCommandService::class.java)

    override fun generateToken(command: GenerateTokenCommand): String {
        log.info("대기열 토큰 생성: userId=${command.userId}, concertId=${command.concertId}")

        val user = userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        val existingToken = queueTokenRepository.findActiveTokenByUserAndConcert(
            command.userId, command.concertId
        )

        if (existingToken != null) {
            log.info("기존 토큰 반환: tokenId=${existingToken.tokenId}")

            val currentPosition = calculateCurrentPosition(existingToken.tokenId, command.concertId)
            queueNotificationPort.notifyTokenGenerated(
                tokenId = existingToken.tokenId,
                userId = command.userId,
                concertId = command.concertId,
                initialPosition = currentPosition
            )

            return existingToken.queueTokenId
        }

        val newToken = queueDomainService.createNewToken(command.userId, command.concertId)
        val savedToken = queueTokenRepository.save(newToken)

        val initialPosition = calculateCurrentPosition(savedToken.queueTokenId, command.concertId)

        queueNotificationPort.notifyTokenGenerated(
            tokenId = savedToken.queueTokenId,
            userId = command.userId,
            concertId = command.concertId,
            initialPosition = initialPosition
        )

        log.info("새 토큰 생성 완료: tokenId=${savedToken.queueTokenId}, position=$initialPosition")
        return savedToken.queueTokenId
    }

    override fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult {
        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        return when (token.tokenStatus) {
            QueueTokenStatus.ACTIVE -> {
                log.debug("활성 토큰 검증 성공: ${command.tokenId}")
                ValidateTokenResult(
                    tokenId = token.queueTokenId,
                    userId = token.userId,
                    concertId = token.concertId,
                    isValid = true
                )
            }
            else -> {
                log.warn("비활성 토큰 검증 실패: ${command.tokenId}, status=${token.tokenStatus}")
                throw InvalidTokenStatusException(command.tokenId, token.tokenStatus)
            }
        }
    }

    override fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult {
        return validateActiveToken(command)
    }

    override fun expireToken(command: ExpireTokenCommand): ExpireTokenResult {
        log.info("토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val expiredToken = token.copy(tokenStatus = QueueTokenStatus.EXPIRED)
        queueTokenRepository.save(expiredToken)

        queueNotificationPort.notifyTokenExpired(
            tokenId = command.tokenId,
            userId = token.userId,
            concertId = token.concertId,
            reason = "토큰이 만료되었습니다"
        )

        log.info("토큰 만료 완료: tokenId=${command.tokenId}")
        return ExpireTokenResult(success = true)
    }

    override fun completeToken(command: CompleteTokenCommand): CompleteTokenResult {
        log.info("토큰 완료 처리: tokenId=${command.tokenId}")

        val token = queueTokenRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val completedToken = token.copy(tokenStatus = QueueTokenStatus.COMPLETED)
        queueTokenRepository.save(completedToken)

        // 토큰 완료 알림 (기존 로직에 추가)
        queueNotificationPort.notifyTokenExpired(
            tokenId = command.tokenId,
            userId = token.userId,
            concertId = token.concertId,
            reason = "예약이 완료되었습니다"
        )

        log.info("토큰 완료: tokenId=${command.tokenId}")
        return CompleteTokenResult(success = true)
    }

    /**
     * 현재 대기열 위치 계산 (기존 로직 활용)
     */
    private fun calculateCurrentPosition(tokenId: String, concertId: Long): Int {
        return try {
            val waitingTokens = queueTokenRepository.findWaitingTokensByConcert(concertId)
            val tokenIndex = waitingTokens.indexOfFirst { it.queueTokenId == tokenId }
            if (tokenIndex >= 0) tokenIndex else 0
        } catch (e: Exception) {
            log.warn("위치 계산 실패: tokenId=$tokenId", e)
            0
        }
    }
}