package kr.hhplus.be.server.domain.log.pointHistory.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException

class PointHistoryNotFoundException private constructor(
    identifier: String
) : EntityNotFoundException("PointHistory", identifier) {

    companion object {
        fun byId(pointHistoryId: Long): PointHistoryNotFoundException {
            return PointHistoryNotFoundException("id: $pointHistoryId")
        }

        fun byUserId(userId: String): PointHistoryNotFoundException {
            return PointHistoryNotFoundException("userId: $userId")
        }
    }
}