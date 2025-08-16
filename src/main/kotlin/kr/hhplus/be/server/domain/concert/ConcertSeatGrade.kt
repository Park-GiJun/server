package kr.hhplus.be.server.domain.concert

import java.time.LocalDateTime

class ConcertSeatGrade(
    val concertSeatGradeId: Long = 0L,
    val concertId: Long,
    val seatGrade: String,
    val price: Int,

    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)