package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.queue.command.GenerateTokenCommand


interface GenerateTokenUseCase {
    fun generateToken(command: GenerateTokenCommand): String
}