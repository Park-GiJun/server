package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.ValidateTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.ValidateTokenResult

interface ValidateTokenUseCase {
    fun validateActiveToken(command: ValidateTokenCommand): ValidateTokenResult
    fun validateActiveTokenForConcert(command: ValidateTokenCommand): ValidateTokenResult
}