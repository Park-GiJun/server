package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import java.time.Duration

/**
 * 대기열 저장소 포트
 */
interface QueueRepository {

    /**
     * 대기열에 토큰 추가
     * @param token 추가할 토큰
     * @return 대기열에서의 위치 (0부터 시작)
     */
    suspend fun addToQueue(token: QueueToken): Long

    /**
     * 토큰 조회
     * @param tokenId 토큰 ID
     * @return 토큰 정보 (없으면 null)
     */
    suspend fun findByTokenId(tokenId: String): QueueToken?

    /**
     * 사용자의 특정 콘서트 토큰 조회
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @return 토큰 정보 (없으면 null)
     */
    suspend fun findByUserAndConcert(userId: String, concertId: Long): QueueToken?

    /**
     * 대기열에서의 현재 위치 조회
     * @param tokenId 토큰 ID
     * @return 위치 (null이면 대기열에 없음)
     */
    suspend fun getQueuePosition(tokenId: String): Long?

    /**
     * 활성 토큰으로 승격
     * @param tokenId 토큰 ID
     * @param ttl 활성 상태 유지 시간
     */
    suspend fun activateToken(tokenId: String, ttl: Duration)

    /**
     * 토큰 상태 업데이트
     * @param tokenId 토큰 ID
     * @param status 새로운 상태
     */
    suspend fun updateTokenStatus(tokenId: String, status: QueueTokenStatus)

    /**
     * 토큰 삭제 (만료 처리)
     * @param tokenId 토큰 ID
     */
    suspend fun removeToken(tokenId: String)

    /**
     * 콘서트별 활성 토큰 수 조회
     * @param concertId 콘서트 ID
     * @return 활성 토큰 수
     */
    suspend fun countActiveTokens(concertId: Long): Int

    /**
     * 콘서트별 대기 토큰 수 조회
     * @param concertId 콘서트 ID
     * @return 대기 토큰 수
     */
    suspend fun countWaitingTokens(concertId: Long): Long

    /**
     * 다음 순서 토큰들 활성화 (배치 처리)
     * @param concertId 콘서트 ID
     * @param count 활성화할 토큰 수
     * @param ttl 활성 상태 유지 시간
     * @return 활성화된 토큰 ID 목록
     */
    suspend fun activateNextTokens(concertId: Long, count: Int, ttl: Duration): List<String>

    /**
     * 만료된 토큰들 정리
     * @return 정리된 토큰 수
     */
    suspend fun cleanupExpiredTokens(): Int
}