package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.UserResult

interface ChargeUserPointUseCase {
    fun chargeUserPoint(command: ChargeUserPointCommand): UserResult
}