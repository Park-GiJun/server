package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UserResult

interface GetUserUseCase {
    fun getUser(command: GetUserCommand): UserResult
}