package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper

import kr.hhplus.be.server.application.dto.queue.GenerateTokenCommand
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.QueueStatusResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse

object QueueWebMapper {

    fun toGenerateTokenCommand(request: GenerateTokenRequest, concertId: Long): GenerateTokenCommand {
        return GenerateTokenCommand(
            userId = request.userId,
            concertId = concertId
        )
    }

    fun toGetQueueStatusQuery(tokenId: String): GetQueueStatusQuery {
        return GetQueueStatusQuery(tokenId = tokenId)
    }

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