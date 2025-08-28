package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.command.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.result.UserResult

interface UseUserPointUseCase {
    fun useUserPoint(command: UseUserPointCommand): UserResult
}