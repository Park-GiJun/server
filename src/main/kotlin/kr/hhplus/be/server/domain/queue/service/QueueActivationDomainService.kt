package kr.hhplus.be.server.domain.queue.service

import org.slf4j.LoggerFactory

class QueueActivationDomainService {

    private val log = LoggerFactory.getLogger(QueueActivationDomainService::class.java)

    companion object {
        private const val MAX_ACTIVE_USERS_PER_CONCERT = 5
    }

    fun calculateTokensToActivate(activateTokenCount : Int) : Int {
        val slotsAvailable = MAX_ACTIVE_USERS_PER_CONCERT - activateTokenCount
        return if (slotsAvailable > 0) slotsAvailable else 0
    }

    fun shouldActivateTokens(activeTokenCount: Int): Boolean {
        return activeTokenCount < MAX_ACTIVE_USERS_PER_CONCERT
    }
}