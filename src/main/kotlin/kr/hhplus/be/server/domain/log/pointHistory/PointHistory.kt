package kr.hhplus.be.server.domain.log.pointHistory

import java.time.LocalDateTime

class PointHistory(
    val pointHistoryId : Long,
    val userId : String,
    val pointHistoryType : String,
    val pointHistoryAmount : Int,
    val description : String,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var isDeleted: Boolean = false,
    var deletedAt: LocalDateTime? = null
)