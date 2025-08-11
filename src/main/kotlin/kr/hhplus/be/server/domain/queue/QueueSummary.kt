package kr.hhplus.be.server.domain.queue

data class QueueSummary(
    val concertId: Long,
    val waitingCount: Int,
    val activeCount: Int,
    val expiredCount: Int,
    val completedCount: Int,
    val activationCapacity: Int,
    val totalCount: Int
) {
    val utilizationRate: Double = if (totalCount > 0) activeCount.toDouble() / totalCount else 0.0
}