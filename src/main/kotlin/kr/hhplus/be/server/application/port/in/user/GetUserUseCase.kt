package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.query.GetUserCommand
import kr.hhplus.be.server.application.dto.user.result.UserResult

interface GetUserUseCase {
    fun getUser(command: GetUserCommand): UserResult
}