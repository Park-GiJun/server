package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.CompleteQueueTokenResult

interface CompleteQueueTokenUseCase {
    fun completeToken(command: CompleteQueueTokenCommand): CompleteQueueTokenResult
}