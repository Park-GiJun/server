package kr.hhplus.be.server.domain.queue

enum class QueueTokenStatus {
    WAITING, ACTIVE, CANCELLED, EXPIRED, COMPLETED, DISCONNECT;

    fun isWaiting(): Boolean = this === WAITING
    fun isActivated(): Boolean = this === ACTIVE
    fun isCanceled(): Boolean = this === CANCELLED
    fun isExpired(): Boolean = this === EXPIRED
    fun isCompleted(): Boolean = this === COMPLETED
    fun isDisConnect(): Boolean = this === DISCONNECT
    fun isFinished(): Boolean = this === CANCELLED || this === EXPIRED || this === COMPLETED
}