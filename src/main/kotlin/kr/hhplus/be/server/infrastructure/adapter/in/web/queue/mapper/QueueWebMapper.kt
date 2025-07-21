package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper

import kr.hhplus.be.server.application.dto.queue.result.QueueStatusResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse

object QueueWebMapper {

    fun toGenerateTokenResponse(tokenId: String): GenerateTokenResponse {
        return GenerateTokenResponse(
            tokenId = tokenId,
            message = "Queue token generated successfully"
        )
    }

    fun toStatusResponse(result: QueueStatusResult): QueueStatusResponse {
        return QueueStatusResponse(
            tokenId = result.tokenId,
            userId = result.userId,
            concertId = result.concertId,
            status = result.status,
            position = result.position
        )
    }
}