package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.ProcessQueueActivationResult

interface ProcessQueueActivationUseCase {
    suspend fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult
}