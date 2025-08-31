package kr.hhplus.be.server.application.port.`in`.user

import kr.hhplus.be.server.application.dto.user.command.ChargeUserPointCommand
import kr.hhplus.be.server.application.dto.user.result.UserResult

interface ChargeUserPointUseCase {
    fun chargeUserPoint(command: ChargeUserPointCommand): UserResult
}