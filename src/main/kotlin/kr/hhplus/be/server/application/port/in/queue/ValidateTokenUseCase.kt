package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.domain.queue.QueueToken

interface ValidateTokenUseCase {
    fun validateActiveToken(tokenId: String): QueueToken
    fun validateActiveTokenForConcert(tokenId: String, concertId: Long): QueueToken
}