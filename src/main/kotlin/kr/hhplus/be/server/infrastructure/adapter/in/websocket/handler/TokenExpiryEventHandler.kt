package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.handler

import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.event.TokenExpiredByDisconnectionEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class TokenExpiryEventHandler(
    private val expireTokenUseCase: ExpireTokenUseCase
) {
    private val log = LoggerFactory.getLogger(TokenExpiryEventHandler::class.java)

    @Async
    @EventListener
    fun handleTokenExpiredByDisconnection(event: TokenExpiredByDisconnectionEvent) {
        try {
            expireTokenUseCase.expireToken(ExpireTokenCommand(event.tokenId))
        } catch (e: Exception) {
            log.warn("Failed to expire token ${event.tokenId} due to ${event.reason}: ${e.message}")
        }
    }
}