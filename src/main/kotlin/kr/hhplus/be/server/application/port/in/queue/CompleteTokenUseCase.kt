package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.CompleteTokenCommand

interface CompleteTokenUseCase {
    fun completeToken(command: CompleteTokenCommand): Boolean
}
