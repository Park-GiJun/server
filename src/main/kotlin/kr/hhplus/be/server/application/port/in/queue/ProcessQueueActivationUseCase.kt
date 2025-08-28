package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.ProcessQueueActivationCommand
import kr.hhplus.be.server.application.dto.queue.result.ProcessQueueActivationResult

interface ProcessQueueActivationUseCase {
    fun processActivation(command: ProcessQueueActivationCommand): ProcessQueueActivationResult
}