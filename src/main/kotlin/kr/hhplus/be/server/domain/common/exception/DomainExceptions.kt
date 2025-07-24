package kr.hhplus.be.server.domain.common.exception

abstract class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

abstract class BusinessRuleViolationException(message: String) : DomainException(message)

abstract class EntityStateException(message: String) : DomainException(message)

abstract class EntityNotFoundException(entityType: String, identifier: String) :
    DomainException("$entityType not found with identifier: $identifier")