package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult

interface UseUserPointUseCase {
    fun useUserPoint(command: UseUserPointCommand): UserResult
}