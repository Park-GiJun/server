package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.CompleteQueueTokenResult

interface CompleteQueueTokenUseCase {
    fun completeToken(command: CompleteQueueTokenCommand): CompleteQueueTokenResult
}