package kr.hhplus.be.server.application.dto.lock

data class LockEvent(
    val lockKey: String,
    val lockType: LockType,
    val eventType: LockEventType,
    val holderId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun acquired(lockKey: String, lockType: LockType, holderId: String): LockEvent {
            return LockEvent(lockKey, lockType, LockEventType.ACQUIRED, holderId)
        }

        fun released(lockKey: String, lockType: LockType): LockEvent {
            return LockEvent(lockKey, lockType, LockEventType.RELEASED)
        }

        fun failed(lockKey: String, lockType: LockType, holderId: String? = null): LockEvent {
            return LockEvent(lockKey, lockType, LockEventType.FAILED, holderId)
        }
    }
}
