package kr.hhplus.be.server.application.mapper

import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.application.dto.queue.ValidateTokenResult
import kr.hhplus.be.server.domain.queue.QueueToken

object QueueMapper {

    fun toStatusResult(domain: QueueToken, position: Int): QueueStatusResult {
        return QueueStatusResult(
            tokenId = domain.queueTokenId,
            userId = domain.userId,
            concertId = domain.concertId,
            status = domain.tokenStatus,
            position = position
        )
    }

    fun toValidateResult(domain: QueueToken, isValid: Boolean): ValidateTokenResult {
        return ValidateTokenResult(
            tokenId = domain.queueTokenId,
            userId = domain.userId,
            concertId = domain.concertId,
            isValid = isValid
        )
    }
}