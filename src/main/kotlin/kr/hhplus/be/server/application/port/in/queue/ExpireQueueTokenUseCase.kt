package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.ExpireQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ExpireQueueTokenResult

interface ExpireQueueTokenUseCase {
    fun expireToken(command: ExpireQueueTokenCommand): ExpireQueueTokenResult
}