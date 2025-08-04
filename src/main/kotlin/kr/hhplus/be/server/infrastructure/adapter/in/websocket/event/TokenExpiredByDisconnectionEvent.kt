
package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.event

data class TokenExpiredByDisconnectionEvent(
    val tokenId: String,
    val reason: String = "WebSocket disconnection"
)