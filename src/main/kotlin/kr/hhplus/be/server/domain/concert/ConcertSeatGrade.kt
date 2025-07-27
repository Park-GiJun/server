package kr.hhplus.be.server.domain.concert

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

class ConcertSeatGrade(
    val concertSeatGradeId: Long = 0L,
    val concertId: Long,
    val seatGrade: String,
    val price: Int
) : BaseEntity()