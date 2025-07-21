package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.application.dto.command.GenerateTokenCommand


interface GenerateTokenUseCase {
    fun generateToken(command: GenerateTokenCommand): String
}