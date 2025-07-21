package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.command.ExpireTokenCommand

interface ExpireTokenUseCase {
    fun expireToken(command: ExpireTokenCommand): Boolean
}
