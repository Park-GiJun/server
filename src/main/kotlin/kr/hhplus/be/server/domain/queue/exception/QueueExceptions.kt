package kr.hhplus.be.server.domain.queue.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException
import kr.hhplus.be.server.domain.common.exception.EntityStateException
import kr.hhplus.be.server.domain.common.exception.BusinessRuleViolationException
import kr.hhplus.be.server.domain.queue.QueueTokenStatus

class QueueTokenNotFoundException(tokenId: String) : EntityNotFoundException("QueueToken", tokenId)

class InvalidTokenStatusException(
    currentStatus: QueueTokenStatus,
    requiredStatus: QueueTokenStatus
) : EntityStateException("Token status is $currentStatus, but $requiredStatus is required")

class InvalidTokenException(reason: String) : BusinessRuleViolationException("Invalid token: $reason")

class TokenExpiredException(tokenId: String) : EntityStateException("Token $tokenId has expired")