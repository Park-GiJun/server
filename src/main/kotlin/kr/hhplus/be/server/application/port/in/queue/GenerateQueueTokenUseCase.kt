package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.GenerateQueueTokenResult

interface GenerateQueueTokenUseCase {
    suspend fun generateToken(command: GenerateQueueTokenCommand): GenerateQueueTokenResult
}