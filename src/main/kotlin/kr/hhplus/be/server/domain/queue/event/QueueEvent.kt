package kr.hhplus.be.server.domain.queue.event

import java.time.LocalDateTime

sealed class QueueEvent {
    abstract val tokenId: String
    abstract val userId: Long
    abstract val concertId: Long
    abstract val timestamp: LocalDateTime

    data class QueueEntered(
        override val tokenId: String,
        override val userId: Long,
        override val concertId: Long,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : QueueEvent()

    data class QueueActivated(
        override val tokenId: String,
        override val userId: Long,
        override val concertId: Long,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : QueueEvent()

    data class QueueCompleted(
        override val tokenId: String,
        override val userId: Long,
        override val concertId: Long,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : QueueEvent()

    data class QueueExpired(
        override val tokenId: String,
        override val userId: Long,
        override val concertId: Long,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : QueueEvent()
}
