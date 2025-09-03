package kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue

import kr.hhplus.be.server.application.dto.event.queue.command.EnterQueueCommand
import kr.hhplus.be.server.application.dto.event.queue.query.GetQueueStatusKafkaQuery
import kr.hhplus.be.server.application.port.`in`.queue.*
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.EnterQueueKafkaRequest
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.EnterQueueKafkaResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusKafkaResponse
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.queue.dto.QueueStatusResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/kafka/queue")
class QueueWebAdapter(
    private val enterQueueUseCase: EnterQueueUseCase,
    private val getQueueStatusKafkaUseCase: GetQueueStatusKafkaUseCase
) {

    @PostMapping("/enter")
    fun enterQueue(@RequestBody request: EnterQueueKafkaRequest): EnterQueueKafkaResponse {
        val command = EnterQueueCommand(
            userId = request.userId,
            concertId = request.concertId
        )

        val result = enterQueueUseCase.enterQueue(command)

        return EnterQueueKafkaResponse(
            tokenId = result.tokenId,
            position = result.position,
            status = result.status,
            message = when (result.status) {
                "ACTIVE" -> "즉시 예약 가능합니다"
                "WAITING" -> "대기열 ${result.position}번째입니다"
                else -> "대기열에 진입했습니다"
            }
        )
    }

    @GetMapping("/status")
    fun getQueueStatus(
        @RequestParam tokenId: String,
        @RequestParam concertId: Long
    ): QueueStatusKafkaResponse {
        val query = GetQueueStatusKafkaQuery(
            tokenId = tokenId,
            concertId = concertId
        )

        val result = getQueueStatusKafkaUseCase.getQueueStatus(query)

        return QueueStatusKafkaResponse(
            tokenId = result.tokenId,
            position = result.position,
            activeCount = result.activeCount,
            waitingCount = result.waitingCount,
            status = result.status,
            estimatedWaitTime = result.position * 30
        )
    }
}