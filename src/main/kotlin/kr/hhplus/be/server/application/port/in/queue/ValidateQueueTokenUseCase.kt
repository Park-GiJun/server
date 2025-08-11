package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.ValidateQueueTokenResult

interface ValidateQueueTokenUseCase {
    suspend fun validateToken(command: ValidateQueueTokenCommand): ValidateQueueTokenResult
}