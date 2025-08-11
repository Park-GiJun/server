package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.ExpireQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.ExpireQueueTokenResult

interface ExpireQueueTokenUseCase {
    suspend fun expireToken(command: ExpireQueueTokenCommand): ExpireQueueTokenResult
}