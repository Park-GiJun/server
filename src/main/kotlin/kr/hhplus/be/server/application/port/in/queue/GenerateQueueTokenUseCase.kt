package kr.hhplus.be.server.application.port.`in`.queue


interface GenerateQueueTokenUseCase {
    suspend fun generateToken(command: GenerateQueueTokenCommand): GenerateQueueTokenResult
}