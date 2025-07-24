package kr.hhplus.be.server.application.port.out.log

import kr.hhplus.be.server.domain.log.PointHistory

interface PointHistoryRepository {
    fun save(pointHistory: PointHistory): PointHistory
    fun findByUserId(userId: String): List<PointHistory>
}