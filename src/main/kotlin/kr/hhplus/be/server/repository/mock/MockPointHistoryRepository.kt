package kr.hhplus.be.server.repository.mock

import kr.hhplus.be.server.domain.log.PointHistory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class MockPointHistoryRepository {
    private val log = LoggerFactory.getLogger(MockPointHistoryRepository::class.java)
    private val pointHistories = ConcurrentHashMap<Long, PointHistory>()
    private val idGenerator = AtomicLong(1)

    fun save(pointHistory: PointHistory): PointHistory {
        val newPointHistory = if (pointHistory.pointHistoryId == 0L) {
            PointHistory(
                pointHistoryId = idGenerator.getAndIncrement(),
                userId = pointHistory.userId,
                pointHistoryType = pointHistory.pointHistoryType,
                pointHistoryAmount = pointHistory.pointHistoryAmount,
                description = pointHistory.description
            )
        } else {
            pointHistory
        }

        pointHistories[newPointHistory.pointHistoryId] = newPointHistory
        log.info("Saved point history: ${newPointHistory.pointHistoryId}")
        return newPointHistory
    }

}