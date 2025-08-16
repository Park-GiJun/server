package kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.jpa

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.entity.PointHistoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointHistoryJpaRepository : JpaRepository<PointHistoryJpaEntity, Long> {
    fun findByUserId(userId: String): List<PointHistoryJpaEntity>?
}