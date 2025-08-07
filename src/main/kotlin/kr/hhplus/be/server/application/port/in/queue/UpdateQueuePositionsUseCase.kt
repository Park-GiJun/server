package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.UpdateQueuePositionsCommand
import kr.hhplus.be.server.application.dto.queue.UpdateQueuePositionsResult

interface UpdateQueuePositionsUseCase {
    fun updateQueuePositions(command: UpdateQueuePositionsCommand): UpdateQueuePositionsResult
}