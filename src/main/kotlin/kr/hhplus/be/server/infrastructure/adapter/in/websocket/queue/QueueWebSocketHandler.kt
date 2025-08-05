package kr.hhplus.be.server.infrastructure.adapter.`in`.websocket.queue

import kr.hhplus.be.server.application.dto.queue.ExpireTokenCommand
import kr.hhplus.be.server.application.dto.queue.GetQueueStatusQuery
import kr.hhplus.be.server.application.port.`in`.queue.ExpireTokenUseCase
import kr.hhplus.be.server.application.port.`in`.queue.GetQueueStatusUseCase
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketMessage
import kr.hhplus.be.server.application.port.out.websocket.queue.QueueWebSocketSessionPort
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.domain.websocket.queue.QueueWebSocketSession
import kr.hhplus.be.server.infrastructure.adapter.out.websocket.queue.SpringWebSocketMessageAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class QueueWebSocketHandler(
    private val webSocketSessionPort: QueueWebSocketSessionPort,
    private val springWebSocketMessageAdapter: SpringWebSocketMessageAdapter,
    private val expireTokenUseCase: ExpireTokenUseCase,
    private val getQueueStatusUseCase: GetQueueStatusUseCase
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(QueueWebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        var tokenId: String? = null
        try {
            log.info("WebSocket 연결 시도: sessionId=${session.id}")

            val connectionInfo = createConnectionInfo(session)

            if (connectionInfo == null) {
                log.warn("유효하지 않은 연결 정보: sessionId=${session.id}")
                session.close(CloseStatus.BAD_DATA.withReason("Invalid connection parameters"))
                return
            }

            tokenId = connectionInfo.tokenId
            log.info("연결 정보 파싱 성공: tokenId=$tokenId, userId=${connectionInfo.userId}, concertId=${connectionInfo.concertId}")

            try {
                val queueStatus = getQueueStatusUseCase.getQueueStatus(GetQueueStatusQuery(tokenId))
                log.info("토큰 상태 확인 성공: tokenId=$tokenId, status=${queueStatus.status}, position=${queueStatus.position}")
            } catch (e: Exception) {
                log.warn("토큰 상태 확인 실패 (무시하고 진행): tokenId=$tokenId", e)
            }

            webSocketSessionPort.addSession(connectionInfo)
            log.info("도메인 세션 등록 완료: tokenId=$tokenId")

            springWebSocketMessageAdapter.addWebSocketSession(connectionInfo.tokenId, session)
            log.info("Spring WebSocket 세션 등록 완료: tokenId=$tokenId")

            sendInitialStatusMessage(connectionInfo)

            log.info("WebSocket 연결 성공: tokenId=$tokenId, concertId=${connectionInfo.concertId}")

        } catch (e: Exception) {
            log.error("WebSocket 연결 처리 중 오류: sessionId=${session.id}, tokenId=$tokenId", e)
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Connection processing error"))
            } catch (closeError: Exception) {
                log.error("세션 종료 중 오류", closeError)
            }
        }
    }

    private fun sendInitialStatusMessage(connectionInfo: QueueWebSocketSession) {
        try {
            val message = mapOf(
                "tokenId" to connectionInfo.tokenId,
                "userId" to connectionInfo.userId,
                "concertId" to connectionInfo.concertId,
                "status" to "WAITING",
                "position" to -1,
                "message" to "대기열에 연결되었습니다.",
                "timestamp" to System.currentTimeMillis()
            )

            val success = springWebSocketMessageAdapter.sendMessage(
                connectionInfo.tokenId,
                QueueWebSocketMessage(
                    tokenId = connectionInfo.tokenId,
                    userId = connectionInfo.userId,
                    concertId = connectionInfo.concertId,
                    status = QueueTokenStatus.WAITING,
                    position = -1,
                    message = "대기열에 연결되었습니다."
                )
            )

            if (success) {
                log.info("초기 상태 메시지 전송 성공: tokenId=${connectionInfo.tokenId}")
            } else {
                log.warn("초기 상태 메시지 전송 실패: tokenId=${connectionInfo.tokenId}")
            }

        } catch (e: Exception) {
            log.error("초기 상태 메시지 전송 중 오류: tokenId=${connectionInfo.tokenId}", e)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // 클라이언트로부터 메시지를 받을 필요가 있다면 여기서 처리
        log.debug("WebSocket 메시지 수신: sessionId=${session.id}, message=${message.payload}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val tokenId = session.attributes["tokenId"] as? String
        log.info("WebSocket 연결 종료: sessionId=${session.id}, tokenId=$tokenId, status=$status")

        try {
            if (tokenId != null) {
                // 세션 정리
                webSocketSessionPort.removeSession(tokenId)
                springWebSocketMessageAdapter.removeWebSocketSession(tokenId)
                log.info("세션 정리 완료: tokenId=$tokenId")

                // 토큰 만료 처리 (연결이 정상적으로 종료된 경우에만)
                if (status.code != CloseStatus.NORMAL.code && status.code != CloseStatus.GOING_AWAY.code) {
                    try {
                        expireTokenUseCase.expireToken(ExpireTokenCommand(tokenId))
                        log.info("토큰 만료 처리 완료: tokenId=$tokenId")
                    } catch (e: Exception) {
                        log.error("토큰 만료 처리 실패: tokenId=$tokenId", e)
                    }
                }
            }

        } catch (e: Exception) {
            log.error("WebSocket 연결 종료 처리 중 오류: tokenId=$tokenId", e)
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val tokenId = session.attributes["tokenId"] as? String
        log.error("WebSocket 전송 오류: sessionId=${session.id}, tokenId=$tokenId", exception)

        try {
            if (tokenId != null) {
                webSocketSessionPort.removeSession(tokenId)
                springWebSocketMessageAdapter.removeWebSocketSession(tokenId)
                expireTokenUseCase.expireToken(ExpireTokenCommand(tokenId))
                log.info("전송 오류 후 정리 완료: tokenId=$tokenId")
            }
        } catch (e: Exception) {
            log.error("전송 오류 후 정리 작업 실패: tokenId=$tokenId", e)
        }
    }

    private fun createConnectionInfo(session: WebSocketSession): QueueWebSocketSession? {
        val tokenId = session.attributes["tokenId"] as? String
        val userId = session.attributes["userId"] as? String
        val concertId = session.attributes["concertId"] as? Long

        log.debug("세션 attributes: tokenId=$tokenId, userId=$userId, concertId=$concertId")

        if (tokenId.isNullOrBlank() || userId.isNullOrBlank() || concertId == null || concertId <= 0) {
            log.warn("유효하지 않은 세션 attributes: tokenId=$tokenId, userId=$userId, concertId=$concertId")
            return null
        }

        return QueueWebSocketSession(
            sessionId = session.id,
            tokenId = tokenId,
            userId = userId,
            concertId = concertId
        )
    }
}