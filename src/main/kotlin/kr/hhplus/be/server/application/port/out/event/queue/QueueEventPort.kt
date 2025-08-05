package kr.hhplus.be.server.application.port.out.event.queue

interface QueueEventPort {
    fun publishTokenActivated(tokenId: String, userId: String, concertId: Long, message:String)
    fun publishPositionUpdated(tokenId: String, newPosition: Int, message: String)
}