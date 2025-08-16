package kr.hhplus.be.server.application.dto.lock

enum class LockEventType {
    ACQUIRED,
    RELEASED,
    EXPIRED,
    FAILED
}