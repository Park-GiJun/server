package kr.hhplus.be.server.domain.users.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException
import kr.hhplus.be.server.domain.common.exception.BusinessRuleViolationException

class UserNotFoundException(userId: String) : EntityNotFoundException("User", userId)

class InsufficientPointException(required: Int, available: Int) :
    BusinessRuleViolationException("Insufficient balance. Required: $required, Available: $available")

class InvalidPointAmountException(amount: Int) :
    BusinessRuleViolationException("Point amount must be positive. Given: $amount")