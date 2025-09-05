package kr.hhplus.be.server.domain.queue.service

import org.springframework.stereotype.Component

@Component
class QueuePartitionStrategy {
    companion object {
        const val PARTITION_COUNT = 10
    }

    fun getPartition(concertId: Long): Int {
        return (concertId % PARTITION_COUNT).toInt()
    }
}