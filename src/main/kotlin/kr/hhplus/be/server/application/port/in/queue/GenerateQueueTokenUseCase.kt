package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.GenerateQueueTokenResult

interface GenerateQueueTokenUseCase {
    fun generateToken(command: GenerateQueueTokenCommand): GenerateQueueTokenResult
}
