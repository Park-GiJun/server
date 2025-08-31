package kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.mapper

import kr.hhplus.be.server.application.dto.user.result.UserResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.user.dto.UserPointResponse

object UserWebMapper {

    fun toPointResponse(result: UserResult): UserPointResponse {
        return UserPointResponse(
            userId = result.userId,
            userName = result.userName,
            totalPoint = result.totalPoint,
            availablePoint = result.availablePoint,
            usedPoint = result.usedPoint
        )
    }
}
