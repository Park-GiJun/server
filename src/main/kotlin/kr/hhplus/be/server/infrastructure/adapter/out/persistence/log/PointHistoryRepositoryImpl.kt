package kr.hhplus.be.server.infrastructure.adapter.out.persistence.log

import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.domain.log.PointHistory
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.entity.PointHistoryJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.mock.MockPointHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class PointHistoryRepositoryImpl(
    private val mockPointHistoryRepository: MockPointHistoryRepository
) : PointHistoryRepository {

    override fun save(pointHistory: PointHistory): PointHistory {
        val mockEntity = PointHistoryJpaEntity(
            pointHistoryId = pointHistory.pointHistoryId,
            userId = pointHistory.userId,
            pointHistoryType = pointHistory.pointHistoryType,
            pointHistoryAmount = pointHistory.pointHistoryAmount,
            description = pointHistory.description
        )

        val savedEntity = mockPointHistoryRepository.save(mockEntity)

        return PointHistory(
            pointHistoryId = savedEntity.pointHistoryId,
            userId = savedEntity.userId,
            pointHistoryType = savedEntity.pointHistoryType,
            pointHistoryAmount = savedEntity.pointHistoryAmount,
            description = savedEntity.description,
            createdAt = savedEntity.createdAt,
            updatedAt = savedEntity.updatedAt,
            isDeleted = savedEntity.isDeleted,
            deletedAt = savedEntity.deletedAt
        )
    }

    override fun findByUserId(userId: String): List<PointHistory> {
        val entities = mockPointHistoryRepository.findByUserId(userId)

        return entities.map { entity ->
            PointHistory(
                pointHistoryId = entity.pointHistoryId,
                userId = entity.userId,
                pointHistoryType = entity.pointHistoryType,
                pointHistoryAmount = entity.pointHistoryAmount,
                description = entity.description,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted,
                deletedAt = entity.deletedAt
            )
        }
    }
}