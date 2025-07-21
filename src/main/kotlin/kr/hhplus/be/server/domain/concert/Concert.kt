package kr.hhplus.be.server.domain.concert

import java.time.LocalDateTime

class Concert(
    val concertId: Long,
    val concertName: String = "",
    val location: String = "",
    val description: String = "",

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)