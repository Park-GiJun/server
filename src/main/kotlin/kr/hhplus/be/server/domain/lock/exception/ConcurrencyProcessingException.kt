package kr.hhplus.be.server.domain.lock.exception

import kr.hhplus.be.server.domain.common.exception.BusinessRuleViolationException

class ConcurrencyProcessingException(message: String) : BusinessRuleViolationException(message)