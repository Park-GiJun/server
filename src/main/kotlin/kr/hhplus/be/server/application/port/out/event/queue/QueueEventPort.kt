package kr.hhplus.be.server.application.port.out.event.queue

interface QueueEventPort {
    fun publishTokenActivated(tokenId: String, userId: String, concertId: Long)
    fun publishTokenExpired(tokenId: String)
    fun publishPositionUpdated(tokenId: String, newPosition: Int, concertId: Long)
}