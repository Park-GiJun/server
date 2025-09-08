package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.event.queue.command.EnterQueueCommand
import kr.hhplus.be.server.application.dto.event.queue.result.EnterQueueResult

interface EnterQueueUseCase {
    fun enterQueue(command: EnterQueueCommand): EnterQueueResult
}

