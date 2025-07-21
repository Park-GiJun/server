package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.ExpireTokenCommand

interface ExpireTokenUseCase {
    fun expireToken(command: ExpireTokenCommand): Boolean
}
