package kr.hhplus.be.server.application.service.queue

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.port.`in`.queue.ProcessQueueActivationUseCase
import kr.hhplus.be.server.application.port.out.queue.QueueTokenRepository
import org.springframework.stereotype.Service

@Service
@Transactional
class QueueActivationService(
    private val queueTokenRepository: QueueTokenRepository,
    private val queueEventPort: QueueEventPort
) : ProcessQueueActivationUseCase {
}