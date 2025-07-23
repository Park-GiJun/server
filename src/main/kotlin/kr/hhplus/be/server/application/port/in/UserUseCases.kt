package kr.hhplus.be.server.application.port.`in`

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.GetUserCommand
import kr.hhplus.be.server.application.dto.user.UseUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult

interface GetUserUseCase {
    fun getUser(command: GetUserCommand): UserResult
}

interface ChargeUserPointUseCase {
    fun chargeUserPoint(command: ChargeUserPointCommand): UserResult
}

interface UseUserPointUseCase {
    fun useUserPoint(command: UseUserPointCommand): UserResult
}