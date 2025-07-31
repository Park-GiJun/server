package kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.repositoryImpl

import kr.hhplus.be.server.application.port.out.log.PointHistoryRepository
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.jpa.PointHistoryJpaRepository
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper.PersistenceMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class PointHistoryRepositoryImpl(
    private val pointHistoryJpaRepository: PointHistoryJpaRepository
) : PointHistoryRepository {

    override fun save(pointHistory: PointHistory): PointHistory {
        return PersistenceMapper.toPointHistoryEntity(pointHistory)
            .let { pointHistoryJpaRepository.save(it) }
            .let { PersistenceMapper.toPointHistoryDomain(it) }
    }

    override fun findByUserId(userId: String): List<PointHistory> {
        return pointHistoryJpaRepository.findByUserId(userId)
            ?.map { PersistenceMapper.toPointHistoryDomain(it) }
            ?: emptyList()
    }
}