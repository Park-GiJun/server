package kr.hhplus.be.server.domain.concert

import java.time.LocalDateTime

class ConcertDate(
    val concertDateId: Long = 0L,
    val concertSession: Long,
    val concertId: Long,
    val date: LocalDateTime,
    val isSoldOut: Boolean = false,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)