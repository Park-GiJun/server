package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.mapper

import kr.hhplus.be.server.application.dto.queue.command.GenerateQueueTokenCommand
import kr.hhplus.be.server.application.dto.queue.result.GenerateQueueTokenResult
import kr.hhplus.be.server.application.dto.queue.query.GetQueueStatusQuery
import kr.hhplus.be.server.application.dto.queue.result.GetQueueStatusResult
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.GenerateTokenResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse

object QueueWebMapper {

    fun toGenerateQueueTokenCommand(userId: String, concertId: Long) : GenerateQueueTokenCommand {
        return GenerateQueueTokenCommand(
            userId = userId,
            concertId = concertId
        )
    }

    fun toResponse(result: GenerateQueueTokenResult) : GenerateTokenResponse {
        return GenerateTokenResponse(
            tokenId = result.tokenId,
            position = result.position,
            estimatedWaitTime = result.estimatedWaitTime,
            status = result.status.name,
            message = when (result.status.name) {
                "ACTIVE" -> "즉시 예약 가능합니다"
                "WAITING" -> "대기열에 진입했습니다. 순서: ${result.position}번째"
                else -> "토큰이 발급되었습니다"
            }
        )
    }

    fun toGetQueueStatusQuery(tokenId: String) : GetQueueStatusQuery {
        return GetQueueStatusQuery(
            tokenId = tokenId
        )
    }

    fun toResponse(result: GetQueueStatusResult) : QueueStatusResponse {
        return QueueStatusResponse(
            tokenId = result.tokenId,
            userId = result.userId,
            concertId = result.concertId,
            status = result.status.name,
            position = result.position,
            estimatedWaitTime = result.estimatedWaitTime,
            message = when (result.status.name) {
                "ACTIVE" -> "예약 가능 상태입니다"
                "WAITING" -> "대기 중입니다. 순서: ${result.position}번째"
                "EXPIRED" -> "토큰이 만료되었습니다"
                else -> "토큰 상태를 확인하세요"
            }
        )
    }
}