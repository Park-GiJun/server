package kr.hhplus.be.server.application.mapper

import kr.hhplus.be.server.application.dto.user.result.UserResult
import kr.hhplus.be.server.domain.users.User

object UserMapper {

    fun toResult(domain: User): UserResult {
        return UserResult(
            userId = domain.userId,
            userName = domain.userName,
            totalPoint = domain.totalPoint,
            availablePoint = domain.availablePoint,
            usedPoint = domain.usedPoint
        )
    }
}