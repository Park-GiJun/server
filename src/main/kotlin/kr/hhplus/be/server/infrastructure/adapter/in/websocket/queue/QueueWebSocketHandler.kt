// QueueWebSocketHandler.kt - 수정된 버전
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

            // ✅ 토큰 상태 확인을 선택적으로 처리 (연결 실패하지 않도록)
            var initialStatus = QueueTokenStatus.WAITING
            var initialPosition = -1
            var initialMessage = "대기열에 연결되었습니다."

            try {
                val queueStatus = getQueueStatusUseCase.getQueueStatus(GetQueueStatusQuery(tokenId))
                initialStatus = queueStatus.status
                initialPosition = queueStatus.position
                initialMessage = when(queueStatus.status) {
                    QueueTokenStatus.WAITING -> "대기 중입니다. 순서: ${queueStatus.position}"
                    QueueTokenStatus.ACTIVE -> "예약 가능합니다!"
                    QueueTokenStatus.EXPIRED -> "대기열이 만료되었습니다."
                    else -> "대기열 상태를 확인해주세요."
                }
                log.info("토큰 상태 확인 성공: tokenId=$tokenId, status=${queueStatus.status}, position=${queueStatus.position}")
            } catch (e: Exception) {
                log.warn("토큰 상태 확인 실패, 기본값으로 진행: tokenId=$tokenId, error=${e.message}")
                // 상태 확인 실패해도 연결은 유지
            }

            // 도메인 세션 등록
            webSocketSessionPort.addSession(connectionInfo)
            log.info("도메인 세션 등록 완료: tokenId=$tokenId")

            // Spring WebSocket 세션 등록
            springWebSocketMessageAdapter.addWebSocketSession(connectionInfo.tokenId, session)
            log.info("Spring WebSocket 세션 등록 완료: tokenId=$tokenId")

            // 초기 상태 메시지 전송
            sendInitialStatusMessage(connectionInfo, initialStatus, initialPosition, initialMessage)

            log.info("WebSocket 연결 성공: tokenId=$tokenId, concertId=${connectionInfo.concertId}")

        } catch (e: Exception) {
            log.error("WebSocket 연결 처리 중 오류: sessionId=${session.id}, tokenId=$tokenId", e)
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Connection processing error: ${e.message}"))
            } catch (closeError: Exception) {
                log.error("세션 종료 중 오류", closeError)
            }
        }
    }

    private fun sendInitialStatusMessage(
        connectionInfo: QueueWebSocketSession,
        status: QueueTokenStatus,
        position: Int,
        messageText: String
    ) {
        try {
            val message = QueueWebSocketMessage(
                tokenId = connectionInfo.tokenId,
                userId = connectionInfo.userId,
                concertId = connectionInfo.concertId,
                status = status,
                position = position,
                message = messageText
            )

            val success = springWebSocketMessageAdapter.sendMessage(connectionInfo.tokenId, message)

            if (success) {
                log.info("초기 상태 메시지 전송 성공: tokenId=${connectionInfo.tokenId}, status=$status, position=$position")
            } else {
                log.warn("초기 상태 메시지 전송 실패: tokenId=${connectionInfo.tokenId}")
            }

        } catch (e: Exception) {
            log.error("초기 상태 메시지 전송 중 오류: tokenId=${connectionInfo.tokenId}", e)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.debug("WebSocket 메시지 수신: sessionId=${session.id}, message=${message.payload}")

        // ✅ 클라이언트에서 상태 조회 요청을 보낼 수 있도록 처리
        val tokenId = session.attributes["tokenId"] as? String
        if (tokenId != null && message.payload == "GET_STATUS") {
            try {
                val status = getQueueStatusUseCase.getQueueStatus(GetQueueStatusQuery(tokenId))
                val responseMessage = QueueWebSocketMessage(
                    tokenId = tokenId,
                    userId = status.userId,
                    concertId = status.concertId,
                    status = status.status,
                    position = status.position,
                    message = "상태 업데이트"
                )
                springWebSocketMessageAdapter.sendMessage(tokenId, responseMessage)
            } catch (e: Exception) {
                log.error("상태 조회 처리 중 오류: tokenId=$tokenId", e)
            }
        }
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

                // ✅ 정상 종료가 아닌 경우에만 토큰 만료 처리
                if (status.code != CloseStatus.NORMAL.code &&
                    status.code != CloseStatus.GOING_AWAY.code) {
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