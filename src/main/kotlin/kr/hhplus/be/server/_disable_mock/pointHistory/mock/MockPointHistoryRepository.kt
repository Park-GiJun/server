package kr.hhplus.be.server._disable_mock.pointHistory.mock

import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.entity.PointHistoryJpaEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MockPointHistoryRepository {
    private val log = LoggerFactory.getLogger(MockPointHistoryRepository::class.java)
    private val pointHistories = ConcurrentHashMap<Long, PointHistoryJpaEntity>()
    private val idGenerator = AtomicLong(1)

    fun save(pointHistoryJpaEntity: PointHistoryJpaEntity): PointHistoryJpaEntity {
        val newPointHistoryJpaEntity = if (pointHistoryJpaEntity.pointHistoryId == 0L) {
            PointHistoryJpaEntity(
                pointHistoryId = idGenerator.getAndIncrement(),
                userId = pointHistoryJpaEntity.userId,
                pointHistoryType = pointHistoryJpaEntity.pointHistoryType,
                pointHistoryAmount = pointHistoryJpaEntity.pointHistoryAmount,
                description = pointHistoryJpaEntity.description
            )
        } else {
            pointHistoryJpaEntity
        }

        pointHistories[newPointHistoryJpaEntity.pointHistoryId] = newPointHistoryJpaEntity
        log.info("Saved point history: ${newPointHistoryJpaEntity.pointHistoryId}")
        return newPointHistoryJpaEntity
    }

    fun findByUserId(userId: String): List<PointHistoryJpaEntity> {
        return pointHistories.values
            .filter { it.userId == userId }}
}