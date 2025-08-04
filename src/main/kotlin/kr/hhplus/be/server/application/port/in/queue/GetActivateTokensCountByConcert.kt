package kr.hhplus.be.server.application.port.`in`.queue

import kr.hhplus.be.server.domain.queue.QueueToken

interface GetActivateTokensCountByConcert {
    fun getActivateTokensCountByQueue(concertId : Long): Int
}