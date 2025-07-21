package kr.hhplus.be.server.infrastructure.adapter.out.persistence.exception

open class DataAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class DatabaseConnectionException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)

class DataIntegrityViolationException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)

class OptimisticLockException(entityType: String, entityId: String) :
    DataAccessException("Optimistic lock failure for $entityType with id: $entityId")