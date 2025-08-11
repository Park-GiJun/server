package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.ValidateQueueTokenResult

interface ValidateQueueTokenUseCase {
    fun validateActiveToken(command: ValidateQueueTokenCommand): ValidateQueueTokenResult
    fun validateActiveTokenForConcert(command: ValidateQueueTokenCommand): ValidateQueueTokenResult
}