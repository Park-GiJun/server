package kr.hhplus.be.server.application.`in`.queue


interface GenerateTokenUseCase {
    fun generateToken(command: GenerateTokenCommand): String
}