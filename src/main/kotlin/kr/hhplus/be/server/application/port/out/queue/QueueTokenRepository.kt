package kr.hhplus.be.server.application.port.out.queue

import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.queue.QueueTokenStatus

/**
 * 대기열 토큰 저장소 포트 (확장)
 */
interface QueueTokenRepository {

    /**
     * 토큰 저장
     */
    fun save(token: QueueToken): QueueToken

    /**
     * 토큰 ID로 조회
     */
    fun findByTokenId(tokenId: String): QueueToken?

    /**
     * 사용자 ID와 콘서트 ID로 조회
     */
    fun findByUserIdAndConcertId(userId: String, concertId: Long): QueueToken?

    /**
     * 대기 중인 토큰들을 순서대로 활성화
     * @param concertId 콘서트 ID
     * @param count 활성화할 토큰 수
     * @return 활성화된 토큰 목록
     */
    fun activateWaitingTokens(concertId: Long, count: Int): List<QueueToken>

    /**
     * 콘서트별 대기 중인 토큰 조회 (제한된 수)
     * @param concertId 콘서트 ID
     * @param limit 조회할 최대 개수
     * @return 대기 중인 토큰 목록 (진입 순서)
     */
    fun findWaitingTokensByConcert(concertId: Long, limit: Int = 50): List<QueueToken>

    /**
     * 콘서트별 활성 토큰 조회
     * @param concertId 콘서트 ID
     * @return 활성 토큰 목록
     */
    fun findActiveTokensByConcert(concertId: Long): List<QueueToken>

    /**
     * 만료된 토큰들 조회
     * @param concertId 콘서트 ID (null이면 모든 콘서트)
     * @return 만료된 토큰 목록
     */
    fun findExpiredTokensByConcert(concertId: Long? = null): List<QueueToken>

    /**
     * 토큰 상태 업데이트
     * @param tokenId 토큰 ID
     * @param status 새로운 상태
     */
    fun updateTokenStatus(tokenId: String, status: QueueTokenStatus): QueueToken?

    /**
     * 토큰 삭제
     * @param tokenId 토큰 ID
     */
    fun deleteToken(tokenId: String): Boolean

    /**
     * 콘서트별 토큰 상태별 개수 조회
     * @param concertId 콘서트 ID
     * @return 상태별 토큰 개수 맵
     */
    fun countTokensByStatusAndConcert(concertId: Long): Map<QueueTokenStatus, Int>

    /**
     * 활성 토큰 만료 처리
     * @param tokenId 토큰 ID
     * @param reason 만료 사유
     */
    fun expireActiveToken(tokenId: String, reason: String = "시간 만료"): QueueToken?

    /**
     * 사용자별 활성 토큰 존재 여부 확인
     * @param userId 사용자 ID
     * @param concertId 콘서트 ID
     * @return 활성 토큰 존재 여부
     */
    fun hasActiveToken(userId: String, concertId: Long): Boolean

    /**
     * 배치로 토큰 상태 업데이트
     * @param tokenIds 토큰 ID 목록
     * @param status 새로운 상태
     * @return 업데이트된 토큰 수
     */
    fun batchUpdateTokenStatus(tokenIds: List<String>, status: QueueTokenStatus): Int
}