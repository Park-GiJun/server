package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.dto.queue.command.ExpireQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.command.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.command.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.ExpireQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.result.GenerateQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.result.GetQueueStatusResult
import kr.hhplus.be.server.application.dto.queue.result.ProcessQueueActivationResult
import kr.hhplus.be.server.application.dto.queue.result.ValidateQueueTokenResult
import kr.hhplus.be.server.application.port.`in`.queue.ExpireQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GenerateQueueTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.`in`.queue.ValidateQueueTokenUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueNotificationPort
import kr.hhplus.be.server.application.port.out.queue.QueueRepository
import kr.hhplus.be.server.application.port.out.user.UserRepository
import kr.hhplus.be.server.domain.queue.QueueDomainService
import kr.hhplus.be.server.domain.queue.QueueEntryValidation
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.users.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration


/**
 * 대기열 관리 애플리케이션 서비스
 * - 실시간 알림 시스템 통합
 */
@Service
@Transactional
class QueueManagementService(
    private val queueRepository: QueueRepository,
    private val queueNotificationPort: QueueNotificationPort,
    private val userRepository: UserRepository,
    private val queueDomainService: QueueDomainService
) : GenerateQueueTokenUseCase,
    GetQueueStatusUseCase,
    ProcessQueueActivationUseCase,
    ValidateQueueTokenUseCase,
    ExpireQueueTokenUseCase {

    private val log = LoggerFactory.getLogger(QueueManagementService::class.java)

    override suspend fun generateToken(command: GenerateQueueTokenCommand): GenerateQueueTokenResult {
        log.info("대기열 토큰 발급 시작: userId=${command.userId}, concertId=${command.concertId}")

        // 1. 사용자 존재 확인
        userRepository.findByUserId(command.userId)
            ?: throw UserNotFoundException(command.userId)

        // 2. 기존 토큰 확인
        val existingToken = queueRepository.findByUserAndConcert(command.userId, command.concertId)

        // 3. 도메인 서비스로 진입 검증
        return when (val validation = queueDomainService.validateQueueEntry(
            command.userId, command.concertId, existingToken
        )) {
            is QueueEntryValidation.ExistingActive -> {
                log.info("기존 활성 토큰 반환: tokenId=${validation.token.tokenId}")

                // 활성 토큰은 위치 0
                queueNotificationPort.notifyQueueEntry(
                    validation.token.tokenId,
                    command.userId,
                    command.concertId,
                    0L,
                    0
                )

                GenerateQueueTokenResult(
                    tokenId = validation.token.tokenId,
                    position = 0L,
                    estimatedWaitTime = 0,
                    status = QueueTokenStatus.ACTIVE
                )
            }

            is QueueEntryValidation.ExistingWaiting -> {
                log.info("기존 대기 토큰 반환: tokenId=${validation.token.tokenId}")

                // 현재 위치 조회
                val currentPosition = queueRepository.getQueuePosition(validation.token.tokenId) ?: 0L
                val estimatedWaitTime = queueDomainService.calculateEstimatedWaitTime(currentPosition)

                queueNotificationPort.notifyQueueEntry(
                    validation.token.tokenId,
                    command.userId,
                    command.concertId,
                    currentPosition,
                    estimatedWaitTime
                )

                GenerateQueueTokenResult(
                    tokenId = validation.token.tokenId,
                    position = currentPosition,
                    estimatedWaitTime = estimatedWaitTime,
                    status = QueueTokenStatus.WAITING
                )
            }

            is QueueEntryValidation.CreateNew -> {
                log.info("새 대기열 토큰 생성: userId=${command.userId}")

                // 4. 새 토큰 생성
                val newToken = QueueToken.create(command.userId, command.concertId)
                val position = queueRepository.addToQueue(newToken)
                val estimatedWaitTime = queueDomainService.calculateEstimatedWaitTime(position)

                // 5. 진입 알림
                queueNotificationPort.notifyQueueEntry(
                    newToken.tokenId,
                    command.userId,
                    command.concertId,
                    position,
                    estimatedWaitTime
                )

                log.info("새 토큰 생성 완료: tokenId=${newToken.tokenId}, position=$position")

                GenerateQueueTokenResult(
                    tokenId = newToken.tokenId,
                    position = position,
                    estimatedWaitTime = estimatedWaitTime,
                    status = QueueTokenStatus.WAITING
                )
            }
        }
    }

    override suspend fun getQueueStatus(query: GetQueueStatusQuery): GetQueueStatusResult {
        log.debug("대기열 상태 조회: tokenId=${query.tokenId}")

        val token = queueRepository.findByTokenId(query.tokenId)
            ?: throw QueueTokenNotFoundException(query.tokenId)

        val position = when (token.status) {
            QueueTokenStatus.WAITING -> queueRepository.getQueuePosition(query.tokenId) ?: 0L
            QueueTokenStatus.ACTIVE -> 0L
            else -> -1L
        }

        val estimatedWaitTime = if (token.status == QueueTokenStatus.WAITING) {
            queueDomainService.calculateEstimatedWaitTime(position)
        } else 0

        return GetQueueStatusResult(
            tokenId = token.tokenId,
            userId = token.userId,
            concertId = token.concertId,
            status = token.status,
            position = position,
            estimatedWaitTime = estimatedWaitTime
        )
    }

    override suspend fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult {
        log.info("대기열 활성화 처리 시작: concertId=${command.concertId}")

        // 1. 현재 활성 토큰 수 확인
        val currentActiveCount = queueRepository.countActiveTokens(command.concertId)

        // 2. 활성화 가능 용량 계산
        val capacity = queueDomainService.calculateActivationCapacity(currentActiveCount, command.maxActiveTokens)

        if (capacity <= 0) {
            log.debug("활성화 용량 없음: concertId=${command.concertId}")
            return ProcessQueueActivationResult(
                concertId = command.concertId,
                activatedTokenIds = emptyList(),
                activatedCount = 0,
                remainingWaitingCount = queueRepository.countWaitingTokens(command.concertId)
            )
        }

        // 3. 다음 순서 토큰들 활성화
        val activatedTokenIds = queueRepository.activateNextTokens(
            command.concertId,
            capacity,
            Duration.ofMinutes(10) // 활성 상태 10분 유지
        )

        // 4. 활성화 알림
        activatedTokenIds.forEach { tokenId ->
            val token = queueRepository.findByTokenId(tokenId)
            if (token != null) {
                queueNotificationPort.notifyTokenActivated(
                    tokenId = tokenId,
                    userId = token.userId,
                    concertId = command.concertId
                )
            }
        }

        // 5. 나머지 대기자들 위치 업데이트 알림
        updateRemainingQueuePositions(command.concertId)

        val remainingCount = queueRepository.countWaitingTokens(command.concertId)

        log.info("대기열 활성화 완료: concertId=${command.concertId}, activated=${activatedTokenIds.size}")

        return ProcessQueueActivationResult(
            concertId = command.concertId,
            activatedTokenIds = activatedTokenIds,
            activatedCount = activatedTokenIds.size,
            remainingWaitingCount = remainingCount
        )
    }

    override suspend fun validateToken(command: ValidateQueueTokenCommand): ValidateQueueTokenResult {
        log.debug("토큰 검증: tokenId=${command.tokenId}")

        val token = queueRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        val isValid = token.isValid() && token.concertId == command.concertId

        return ValidateQueueTokenResult(
            tokenId = token.tokenId,
            userId = token.userId,
            concertId = token.concertId,
            isValid = isValid,
            status = token.status
        )
    }

    override suspend fun expireToken(command: ExpireQueueTokenCommand): ExpireQueueTokenResult {
        log.info("토큰 만료 처리: tokenId=${command.tokenId}")

        val token = queueRepository.findByTokenId(command.tokenId)
            ?: throw QueueTokenNotFoundException(command.tokenId)

        // 토큰 만료 처리
        queueRepository.updateTokenStatus(command.tokenId, QueueTokenStatus.EXPIRED)

        // 만료 알림
        queueNotificationPort.notifyTokenExpired(
            command.tokenId,
            token.userId,
            token.concertId,
            command.reason
        )

        // 대기열 재정렬
        updateRemainingQueuePositions(token.concertId)

        return ExpireQueueTokenResult(
            tokenId = command.tokenId,
            success = true
        )
    }

    /**
     * 나머지 대기자들의 위치 업데이트 알림
     */
    private suspend fun updateRemainingQueuePositions(concertId: Long) {
        try {
            // 실제 구현은 QueueRepository에서 배치로 처리
            // 여기서는 알림만 트리거
            val waitingCount = queueRepository.countWaitingTokens(concertId)

            if (waitingCount > 0) {
                log.debug("대기열 위치 업데이트 트리거: concertId=$concertId, waiting=$waitingCount")
                // 실제 위치 업데이트 및 알림은 Redis 어댑터에서 처리
            }
        } catch (e: Exception) {
            log.error("대기열 위치 업데이트 실패: concertId=$concertId", e)
        }
    }
}

/**
 * 도메인 예외들
 */
class QueueTokenNotFoundException(tokenId: String) :
    RuntimeException("대기열 토큰을 찾을 수 없습니다: $tokenId")